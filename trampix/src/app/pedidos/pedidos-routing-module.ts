import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { prestadorGuard } from '../core/guards/prestador-guard';
import { MeusPedidos } from './meus-pedidos/meus-pedidos';
import { PedidosRecebidos } from './pedidos-recebidos/pedidos-recebidos';

// O authGuard fica na rota "pedidos" do layout; "recebidos" ainda exige ser prestador.
const routes: Routes = [
  { path: '', component: MeusPedidos },
  { path: 'recebidos', component: PedidosRecebidos, canActivate: [prestadorGuard] },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class PedidosRoutingModule {}
