import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { Auth } from './auth';

describe('Auth', () => {
  let service: Auth;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(Auth);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function responderLogin(expiraEm: string): void {
    service.login({ email: 'ana@teste.com', senha: '12345678' }).subscribe();
    http.expectOne(`${environment.apiUrl}/auth/login`).flush({
      token: 'abc',
      tipo: 'Bearer',
      expiraEm,
      usuario: { id: 1, nome: 'Ana', email: 'ana@teste.com', perfil: 'USUARIO', status: 'ATIVO' },
    });
  }

  it('guarda o token depois do login e limpa no logout', () => {
    responderLogin(new Date(Date.now() + 60_000).toISOString());
    expect(service.token()).toBe('abc');
    expect(service.usuario()?.nome).toBe('Ana');

    service.logout();
    expect(service.token()).toBeNull();
    expect(localStorage.length).toBe(0);
  });

  it('descarta token expirado', () => {
    responderLogin(new Date(Date.now() - 1000).toISOString());
    expect(service.token()).toBeNull();
    expect(service.estaLogado()).toBe(false);
  });
});
