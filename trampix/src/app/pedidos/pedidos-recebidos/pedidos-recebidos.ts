import { Component } from '@angular/core';
import { Contratacao, Papel } from '../../core/models/contratacao';
import { formatarData, hojeEmBrasilia } from '../../core/utils/periodo';
import { ListaPedidos } from '../lista-pedidos';

// "Pedidos recebidos" (prestador): GET /contratacoes?papel=prestador.
// Pendente: aceitar, recusar ou contrapropor. Aceita: concluir (a partir da data de início) ou cancelar.
@Component({
  selector: 'app-pedidos-recebidos',
  standalone: false,
  styleUrl: '../pedidos.scss',
  templateUrl: './pedidos-recebidos.html',
})
export class PedidosRecebidos extends ListaPedidos {
  protected readonly papel: Papel = 'prestador';
  protected readonly formatarData = formatarData;

  aceitar(pedido: Contratacao): void {
    this.executar(
      pedido.id,
      this.contratacaoApi.aceitar(pedido.id),
      'Pedido aceito! O telefone do cliente já aparece no pedido.',
    );
  }

  concluir(pedido: Contratacao): void {
    this.executar(pedido.id, this.contratacaoApi.concluir(pedido.id), 'Serviço concluído.');
  }

  // O back só deixa concluir a partir da data de início (horário de Brasília).
  podeConcluir(pedido: Contratacao): boolean {
    return hojeEmBrasilia() >= pedido.dataInicio;
  }

  contrapropostaEnviada(atualizado: Contratacao): void {
    this.atualizar(atualizado);
    this.fecharPainel();
  }
}
