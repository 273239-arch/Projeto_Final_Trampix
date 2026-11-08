import { Component } from '@angular/core';
import { Contratacao, Papel } from '../../core/models/contratacao';
import { ListaPedidos } from '../lista-pedidos';

// "Meus pedidos" (cliente): GET /contratacoes?papel=cliente.
// Cancelar (pendente ou aceita) e responder à contraproposta do prestador.
@Component({
  selector: 'app-meus-pedidos',
  standalone: false,
  styleUrl: '../pedidos.scss',
  templateUrl: './meus-pedidos.html',
})
export class MeusPedidos extends ListaPedidos {
  protected readonly papel: Papel = 'cliente';

  aceitarContraproposta(pedido: Contratacao): void {
    this.executar(
      pedido.id,
      this.contratacaoApi.aceitarContraproposta(pedido.id),
      'Contraproposta aceita! O telefone do prestador já aparece no pedido.',
    );
  }
}
