import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

// O login fica fora do layout (no Figma, a tela de login não tem cabeçalho).
// As outras páginas são filhas do LayoutModule, que traz cabeçalho e rodapé.
const routes: Routes = [
  {
    path: 'login',
    loadChildren: () => import('./login/login-module').then((m) => m.LoginModule),
  },
  {
    path: '',
    loadChildren: () => import('./layout/layout-module').then((m) => m.LayoutModule),
  },
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
