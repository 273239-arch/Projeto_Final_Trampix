import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { Usuario } from '../models/usuario';
import { Auth } from './auth';
import { Prestador } from './prestador';

describe('Prestador', () => {
  let http: HttpTestingController;
  let service: Prestador;
  const usuario = signal<Partial<Usuario> | null>({ id: 5, nome: 'Ana' });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Auth, useValue: { usuario } },
      ],
    });
    service = TestBed.inject(Prestador);
    http = TestBed.inject(HttpTestingController);
  });

  it('é prestador quando o back responde 200, e guarda a resposta', () => {
    let resposta: boolean | undefined;
    service.verificar().subscribe((eh) => (resposta = eh));
    http.expectOne(`${environment.apiUrl}/usuarios/5/prestador`).flush({ id: 1 });
    expect(resposta).toBe(true);
    expect(service.ehPrestador()).toBe(true);

    service.verificar().subscribe();
    http.expectNone(`${environment.apiUrl}/usuarios/5/prestador`);
  });

  it('não é prestador quando o back responde 404', () => {
    let resposta: boolean | undefined;
    service.verificar().subscribe((eh) => (resposta = eh));
    http
      .expectOne(`${environment.apiUrl}/usuarios/5/prestador`)
      .flush({ status: 404, mensagem: 'este usuário não é prestador' }, { status: 404, statusText: 'Not Found' });
    expect(resposta).toBe(false);
    expect(service.ehPrestador()).toBe(false);
  });
});
