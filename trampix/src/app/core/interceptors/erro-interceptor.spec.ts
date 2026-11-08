import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { Auth } from '../services/auth';
import { erroInterceptor } from './erro-interceptor';

describe('erroInterceptor', () => {
  let http: HttpClient;
  let controle: HttpTestingController;
  const snackBar = { open: vi.fn() };
  const auth = { logout: vi.fn() };

  beforeEach(() => {
    snackBar.open.mockClear();
    auth.logout.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([erroInterceptor])),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: snackBar },
        { provide: Auth, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpClient);
    controle = TestBed.inject(HttpTestingController);
  });

  it('mostra a mensagem do back no snackbar', () => {
    http.post(`${environment.apiUrl}/usuarios`, {}).subscribe({ error: () => {} });
    controle
      .expectOne(`${environment.apiUrl}/usuarios`)
      .flush({ status: 409, mensagem: 'e-mail já cadastrado' }, { status: 409, statusText: 'Conflict' });
    expect(snackBar.open).toHaveBeenCalledWith('E-mail já cadastrado', 'Fechar', expect.anything());
    expect(auth.logout).not.toHaveBeenCalled();
  });

  it('em 401 limpa a sessão e vai para o login', () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    http.get(`${environment.apiUrl}/usuarios/1`).subscribe({ error: () => {} });
    controle
      .expectOne(`${environment.apiUrl}/usuarios/1`)
      .flush({ status: 401, mensagem: 'token inválido ou expirado' }, { status: 401, statusText: 'Unauthorized' });
    expect(auth.logout).toHaveBeenCalled();
    expect(navegar).toHaveBeenCalledWith(['/login'], expect.anything());
  });
});
