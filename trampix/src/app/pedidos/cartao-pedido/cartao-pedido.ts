import { Component, computed, input } from '@angular/core';
import { Contratacao, Endereco, NOME_STATUS, Papel } from '../../core/models/contratacao';
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

  protected readonly nomeStatus = NOME_STATUS;

  protected readonly outraParte = computed(() =>
    this.papel() === 'cliente' ? this.pedido().prestador : this.pedido().cliente,
  );

  // Endereço em duas linhas; null quando o back não liberou.
  protected readonly endereco = computed(() => {
    const e = this.outraParte().endereco;
    return e ? { rua: linhaRua(e), cidade: linhaCidade(e) } : null;
  });

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

// "Rua A, 10 - Apto 2 - Centro".
function linhaRua(e: Endereco): string {
  const rua = [e.logradouro, e.numero].filter(Boolean).join(', ');
  return [rua, e.complemento, e.bairro].filter(Boolean).join(' - ');
}

// "Olinda - PE · CEP 53000-000".
function linhaCidade(e: Endereco): string {
  const cidade = [e.cidade, e.uf].filter(Boolean).join(' - ');
  return [cidade, e.cep ? `CEP ${e.cep}` : null].filter(Boolean).join(' · ');
}
