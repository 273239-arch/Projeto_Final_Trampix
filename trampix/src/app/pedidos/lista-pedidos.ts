import { Directive, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, Subscription } from 'rxjs';
import { Contratacao, Papel } from '../core/models/contratacao';
import { Servico } from '../core/models/servico';
import { ContratacaoApi } from '../core/services/contratacao-api';
import { ServicoApi } from '../core/services/servico-api';

export type Painel = 'cancelar' | 'recusar' | 'contrapropor';

// Base de "Meus pedidos" e "Pedidos recebidos": carrega a lista, guarda o painel aberto
// (cancelar, recusar, contrapropor) e troca o pedido na lista depois de cada ação.
@Directive()
export abstract class ListaPedidos implements OnInit {
  protected abstract readonly papel: Papel;

  protected readonly contratacaoApi = inject(ContratacaoApi);
  private readonly servicoApi = inject(ServicoApi);
  private readonly snackBar = inject(MatSnackBar);
  private carregamento?: Subscription;

  protected readonly pedidos = signal<Contratacao[]>([]);
  protected readonly carregando = signal(false);
  protected readonly erro = signal(false);

  // A contratação traz só id, título e cobrança do serviço: categoria e subcategoria vêm de GET /servicos.
  protected readonly servicos = signal<ReadonlyMap<number, Servico>>(new Map());

  // Pedido com ação em andamento (desabilita os botões dele).
  protected readonly processando = signal<number | null>(null);
  protected readonly painel = signal<{ id: number; tipo: Painel } | null>(null);
  protected readonly motivoRecusa = new FormControl('', {
    nonNullable: true,
    validators: Validators.maxLength(500),
  });

  constructor() {
    inject(DestroyRef).onDestroy(() => this.carregamento?.unsubscribe());
  }

  ngOnInit(): void {
    this.carregar();
    this.carregarServicos();
  }

  carregar(): void {
    this.carregamento?.unsubscribe();
    this.carregando.set(true);
    this.erro.set(false);
    this.carregamento = this.contratacaoApi.listar(this.papel).subscribe({
      next: (pedidos) => {
        this.pedidos.set(pedidos);
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set(true);
        this.carregando.set(false);
      },
    });
  }

  abrirPainel(id: number, tipo: Painel): void {
    this.motivoRecusa.reset('');
    this.painel.set({ id, tipo });
  }

  fecharPainel(): void {
    this.painel.set(null);
  }

  painelAberto(id: number, tipo: Painel): boolean {
    const painel = this.painel();
    return painel?.id === id && painel.tipo === tipo;
  }

  cancelar(pedido: Contratacao): void {
    this.executar(pedido.id, this.contratacaoApi.cancelar(pedido.id), 'Pedido cancelado.');
  }

  recusar(pedido: Contratacao, mensagem: string): void {
    if (this.motivoRecusa.invalid) {
      return;
    }
    const motivo = this.motivoRecusa.value.trim() || null;
    this.executar(pedido.id, this.contratacaoApi.recusar(pedido.id, motivo), mensagem);
  }

  // Troca o pedido da lista pela versão que o back devolveu.
  atualizar(atualizado: Contratacao): void {
    this.pedidos.update((lista) => lista.map((p) => (p.id === atualizado.id ? atualizado : p)));
  }

  // Erros (422 de regra, 403...) aparecem no snackbar do erroInterceptor.
  protected executar(id: number, chamada: Observable<Contratacao>, sucesso: string): void {
    this.processando.set(id);
    chamada.subscribe({
      next: (atualizado) => {
        this.atualizar(atualizado);
        this.processando.set(null);
        this.fecharPainel();
        this.snackBar.open(sucesso, 'Fechar', { duration: 4000 });
      },
      error: () => this.processando.set(null),
    });
  }

  private carregarServicos(): void {
    this.servicoApi.listar({}, true).subscribe({
      next: (lista) => this.servicos.set(new Map(lista.map((s) => [s.id, s]))),
      error: () => undefined,
    });
  }
}
