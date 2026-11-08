import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ErroApi } from '../models/erro-api';
import { Auth } from '../services/auth';

// Para chamadas cujo erro a própria tela mostra (ex.: formulários): não repete no snackbar.
// Uso: http.post(url, corpo, { context: new HttpContext().set(SEM_AVISO_DE_ERRO, true) }).
export const SEM_AVISO_DE_ERRO = new HttpContextToken<boolean>(() => false);

// Mostra a mensagem de erro do back num snackbar. Em 401, encerra a sessão e vai para o login.
// O erro continua sendo repassado, para o componente tratar (ex.: erros por campo no formulário).
export const erroInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);
  const router = inject(Router);
  const auth = inject(Auth);

  return next(req).pipe(
    catchError((erro: unknown) => {
      if (erro instanceof HttpErrorResponse) {
        // O 401 do próprio login é "e-mail ou senha inválidos": só mostra a mensagem.
        const sessaoVencida = erro.status === 401 && !req.url.endsWith('/auth/login');
        if (sessaoVencida || !req.context.get(SEM_AVISO_DE_ERRO)) {
          snackBar.open(mensagemDoErro(erro), 'Fechar', { duration: 5000 });
        }
        if (sessaoVencida) {
          auth.logout();
          const voltar = router.url.startsWith('/login') ? undefined : router.url;
          router.navigate(['/login'], { queryParams: { voltar } });
        }
      }
      return throwError(() => erro);
    }),
  );
};

export function mensagemDoErro(erro: HttpErrorResponse): string {
  if (erro.status === 0) {
    return 'Não foi possível falar com o servidor. Confira se a API está rodando.';
  }
  const corpo = erro.error as Partial<ErroApi> | null;
  const mensagem = corpo?.mensagem ?? `Erro ${erro.status}`;
  // O back manda em minúsculas ("e-mail já cadastrado"): deixa a primeira letra maiúscula.
  return mensagem.charAt(0).toUpperCase() + mensagem.slice(1);
}
