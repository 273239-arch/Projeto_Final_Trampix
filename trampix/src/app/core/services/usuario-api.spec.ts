import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { UsuarioApi } from './usuario-api';

describe('UsuarioApi', () => {
  let service: UsuarioApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UsuarioApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('faz POST /usuarios com o corpo do cadastro', () => {
    const novo = {
      nome: 'Ana',
      email: 'ana@teste.com',
      senha: '12345678',
      tipoDocumento: 'CPF' as const,
      documento: '52998224725',
      telefone: '11999998888',
    };
    service.cadastrar(novo).subscribe();
    const req = http.expectOne(`${environment.apiUrl}/usuarios`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(novo);
    req.flush({});
  });
});
