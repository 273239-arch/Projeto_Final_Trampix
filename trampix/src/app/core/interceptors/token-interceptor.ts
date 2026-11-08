import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Auth } from '../services/auth';

// Coloca "Authorization: Bearer <token>" nas chamadas para a API do Trampix.
export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(environment.apiUrl) || req.url.endsWith('/auth/login')) {
    return next(req);
  }
  const token = inject(Auth).token();
  if (!token) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
