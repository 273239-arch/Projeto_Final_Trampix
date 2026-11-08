import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from '../services/auth';

// Libera a rota só para quem está logado. Senão, vai para o login e volta depois.
export const authGuard: CanActivateFn = (_route, state) => {
  if (inject(Auth).estaLogado()) {
    return true;
  }
  return inject(Router).createUrlTree(['/login'], { queryParams: { voltar: state.url } });
};
