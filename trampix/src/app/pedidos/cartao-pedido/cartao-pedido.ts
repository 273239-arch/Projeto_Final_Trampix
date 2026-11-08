import { Component, computed, input } from '@angular/core';
import { Contratacao, NOME_STATUS, Papel } from '../../core/models/contratacao';
import { Servico } from '../../core/models/servico';
import { descreverPeriodo, formatarData, minutos, quantidadeDias } from '../../core/utils/periodo';

// Cartão de um pedido: status, serviço, a outra parte, datas, preço, observação e motivos.
// As ações (botões e painéis) entram pelo <ng-content>.
@Component({
  selector: 'app-cartao-pedido',
  standalone: false,
  styleUrl: './cartao-pedido.scss',
  templateUrl: './cartao-pedido.html',
})
export class CartaoPedido {
  readonly pedido = input.required<Contratacao>();
  // Quem está vendo: o cliente vê o prestador; o prestador vê o cliente.
  readonly papel = input.required<Papel>();
  // Serviço completo (para categoria e subcategoria). Nulo se o serviço não está mais ativo.
  readonly servico = input<Servico | undefined>(undefined);

  protected readonly nomeStatus = NOME_STATUS;

  protected readonly outraParte = computed(() =>
    this.papel() === 'cliente' ? this.pedido().prestador : this.pedido().cliente,
  );

  protected readonly periodo = computed(() => descreverPeriodo(this.pedido()));

  // "3 h" ou "2 dias".
  protected readonly duracao = computed(() => {
    const p = this.pedido();
    if (p.horaInicio && p.horaFim) {
      return `${(minutos(p.horaFim) - minutos(p.horaInicio)) / 60} h`;
    }
    const dias = quantidadeDias(p.dataInicio, p.dataFim);
    return `${dias} ${dias === 1 ? 'dia' : 'dias'}`;
  });

  protected readonly feitoEm = computed(() => formatarData(this.pedido().criadoEm));

  // Telefone só para ligação: deixa só os dígitos.
  protected readonly linkTelefone = computed(
    () => `tel:${(this.outraParte().telefone ?? '').replace(/\D/g, '')}`,
  );
}
