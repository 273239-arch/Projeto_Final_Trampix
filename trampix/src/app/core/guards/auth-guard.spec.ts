import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  provideRouter,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { Auth } from '../services/auth';
import { authGuard } from './auth-guard';

describe('authGuard', () => {
  let logado: boolean;
  const executeGuard: CanActivateFn = (...guardParameters) =>
    TestBed.runInInjectionContext(() => authGuard(...guardParameters));
  const estado = { url: '/perfil' } as RouterStateSnapshot;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: Auth, useValue: { estaLogado: () => logado } }],
    });
  });

  it('libera quem está logado', () => {
    logado = true;
    expect(executeGuard({} as ActivatedRouteSnapshot, estado)).toBe(true);
  });

  it('manda para o login quem não está logado', () => {
    logado = false;
    const resultado = executeGuard({} as ActivatedRouteSnapshot, estado) as UrlTree;
    expect(resultado.toString()).toBe('/login?voltar=%2Fperfil');
  });
});
