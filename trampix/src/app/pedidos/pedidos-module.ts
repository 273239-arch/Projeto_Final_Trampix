import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { PedidosRoutingModule } from './pedidos-routing-module';
import { CartaoPedido } from './cartao-pedido/cartao-pedido';
import { FormContraproposta } from './form-contraproposta/form-contraproposta';
import { MeusPedidos } from './meus-pedidos/meus-pedidos';
import { PedidosRecebidos } from './pedidos-recebidos/pedidos-recebidos';

@NgModule({
  declarations: [MeusPedidos, PedidosRecebidos, CartaoPedido, FormContraproposta],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    PedidosRoutingModule,
  ],
})
export class PedidosModule {}
