import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { authGuard } from '../core/guards/auth-guard';
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
      // Detalhe público: o login só é pedido ao clicar em "Solicitar serviço".
      {
        path: 'servicos',
        loadChildren: () => import('../servicos/servicos-module').then((m) => m.ServicosModule),
      },
      {
        path: 'pedidos',
        canActivate: [authGuard],
        loadChildren: () => import('../pedidos/pedidos-module').then((m) => m.PedidosModule),
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
