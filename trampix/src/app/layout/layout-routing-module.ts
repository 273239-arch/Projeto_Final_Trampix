import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Layout } from './layout/layout';

// Páginas que aparecem dentro do cabeçalho e do rodapé.
const routes: Routes = [
  {
    path: '',
    component: Layout,
    children: [
      {
        path: 'clientes',
        loadChildren: () => import('../clientes/clientes-module').then((m) => m.ClientesModule),
      },
      {
        path: '',
        loadChildren: () => import('../home/home-module').then((m) => m.HomeModule),
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class LayoutRoutingModule {}
