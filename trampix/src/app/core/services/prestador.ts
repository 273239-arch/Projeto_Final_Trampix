import { HttpClient, HttpContext, HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Service, signal } from '@angular/core';
import { catchError, map, Observable, of, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SEM_AVISO_DE_ERRO } from '../interceptors/erro-interceptor';
import { Auth } from './auth';

// Diz se o usuário logado é prestador (GET /usuarios/{id}/prestador: 200 sim, 404 não).
// A resposta fica guardada por usuário, para não perguntar de novo a cada tela.
@Service()
export class Prestador {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(Auth);
  private readonly conhecidos = signal<Record<number, boolean>>({});

  readonly ehPrestador = computed(() => {
    const usuario = this.auth.usuario();
    return usuario ? this.conhecidos()[usuario.id] === true : false;
  });

  verificar(): Observable<boolean> {
    const usuario = this.auth.usuario();
    if (!usuario) {
      return of(false);
    }
    const conhecido = this.conhecidos()[usuario.id];
    if (conhecido !== undefined) {
      return of(conhecido);
    }
    return this.http
      .get(`${environment.apiUrl}/usuarios/${usuario.id}/prestador`, {
        context: new HttpContext().set(SEM_AVISO_DE_ERRO, true),
      })
      .pipe(
        map(() => true),
        catchError((erro: unknown) =>
          erro instanceof HttpErrorResponse && erro.status === 404 ? of(false) : throwError(() => erro),
        ),
        tap((eh) => this.conhecidos.update((c) => ({ ...c, [usuario.id]: eh }))),
      );
  }
}
