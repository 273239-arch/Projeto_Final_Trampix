import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { Prestador } from '../services/prestador';

// Libera a rota só para prestador (use depois do authGuard). Os outros vão para "Meus pedidos".
export const prestadorGuard: CanActivateFn = () => {
  const router = inject(Router);
  const snackBar = inject(MatSnackBar);
  return inject(Prestador)
    .verificar()
    .pipe(
      catchError(() => of(false)),
      map((eh) => {
        if (eh) {
          return true;
        }
        snackBar.open('Só quem é prestador recebe pedidos.', 'Fechar', { duration: 4000 });
        return router.createUrlTree(['/pedidos']);
      }),
    );
};
