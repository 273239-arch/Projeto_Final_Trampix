import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { map, Subscription } from 'rxjs';
import { Categoria } from '../../core/models/categoria';
import { Servico, SUFIXO_COBRANCA, TipoCobranca } from '../../core/models/servico';
import { CategoriaApi } from '../../core/services/categoria-api';
import { ServicoApi } from '../../core/services/servico-api';

// Home pública: filtro por categoria e subcategoria (GET /categorias) e cartões de serviço (GET /servicos).
// O texto da busca do cabeçalho chega por ?busca= e filtra a lista já carregada.
@Component({
  selector: 'app-home',
  standalone: false,
  styleUrl: './home.scss',
  templateUrl: './home.html',
})
export class Home implements OnInit {
  private readonly categoriaApi = inject(CategoriaApi);
  private readonly servicoApi = inject(ServicoApi);
  private readonly router = inject(Router);
  private carregamentoServicos?: Subscription;

  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly carregandoCategorias = signal(false);
  protected readonly erroCategorias = signal(false);

  protected readonly servicos = signal<Servico[]>([]);
  protected readonly carregandoServicos = signal(false);
  protected readonly erroServicos = signal(false);
  protected readonly categoriaId = signal<number | null>(null);
  protected readonly subcategoriaId = signal<number | null>(null);

  // Subcategorias da categoria escolhida (segunda linha do filtro).
  protected readonly subcategorias = computed(
    () => this.categorias().find((c) => c.id === this.categoriaId())?.subcategorias ?? [],
  );

  protected readonly busca = toSignal(
    inject(ActivatedRoute).queryParamMap.pipe(map((p) => (p.get('busca') ?? '').trim())),
    { initialValue: '' },
  );

  protected readonly servicosFiltrados = computed(() => {
    const termo = normalizar(this.busca());
    if (!termo) {
      return this.servicos();
    }
    return this.servicos().filter((s) =>
      normalizar(
        `${s.titulo} ${s.descricao ?? ''} ${s.categoria.nome} ${s.subcategoria.nome} ${s.prestador.nome}`,
      ).includes(termo),
    );
  });

  constructor() {
    inject(DestroyRef).onDestroy(() => this.carregamentoServicos?.unsubscribe());
  }

  ngOnInit(): void {
    this.carregarCategorias();
    this.carregarServicos();
  }

  carregarCategorias(): void {
    this.carregandoCategorias.set(true);
    this.erroCategorias.set(false);
    this.categoriaApi.listar().subscribe({
      next: (categorias) => {
        // Só as categorias de primeiro nível viram filtro; as subcategorias vêm dentro delas.
        this.categorias.set(
          categorias
            .filter((c) => c.categoriaPaiId == null)
            .sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR')),
        );
        this.carregandoCategorias.set(false);
      },
      error: () => {
        this.erroCategorias.set(true);
        this.carregandoCategorias.set(false);
      },
    });
  }

  carregarServicos(): void {
    // Se o usuário trocar o filtro antes da resposta, a chamada anterior é cancelada.
    this.carregamentoServicos?.unsubscribe();
    this.carregandoServicos.set(true);
    this.erroServicos.set(false);
    this.carregamentoServicos = this.servicoApi
      .listar({ categoriaId: this.categoriaId(), subcategoriaId: this.subcategoriaId() })
      .subscribe({
        next: (servicos) => {
          this.servicos.set(servicos);
          this.carregandoServicos.set(false);
        },
        error: () => {
          this.erroServicos.set(true);
          this.carregandoServicos.set(false);
        },
      });
  }

  filtrarPorCategoria(id: number | null): void {
    if (id === this.categoriaId()) {
      return;
    }
    this.categoriaId.set(id);
    this.subcategoriaId.set(null);
    this.carregarServicos();
  }

  filtrarPorSubcategoria(id: number | null): void {
    if (id === this.subcategoriaId()) {
      return;
    }
    this.subcategoriaId.set(id);
    this.carregarServicos();
  }

  limparBusca(): void {
    this.router.navigate(['/']);
  }

  cobranca(tipo: TipoCobranca): string {
    return SUFIXO_COBRANCA[tipo] ?? '';
  }
}

// Minúsculas e sem acento, para "pintura" achar "Pintura" e "eletrica" achar "Elétrica".
function normalizar(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase();
}
