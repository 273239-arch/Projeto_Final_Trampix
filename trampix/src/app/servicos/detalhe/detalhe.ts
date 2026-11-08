import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { Disponibilidade, NOME_DIA } from '../../core/models/disponibilidade';
import { NOME_COBRANCA, Servico, SUFIXO_COBRANCA } from '../../core/models/servico';
import { Auth } from '../../core/services/auth';
import { DisponibilidadeApi } from '../../core/services/disponibilidade-api';
import { ServicoApi } from '../../core/services/servico-api';
import { formatarHora } from '../../core/utils/periodo';

// Figma "Desktop - 2": título, descrição, preço, categoria e prestador, e o botão "Solicitar serviço".
// Sem galeria de fotos (o back ainda não tem), sem avaliações e sem chat.
@Component({
  selector: 'app-detalhe-servico',
  standalone: false,
  styleUrl: './detalhe.scss',
  templateUrl: './detalhe.html',
})
export class DetalheServico implements OnInit {
  private readonly servicoApi = inject(ServicoApi);
  private readonly disponibilidadeApi = inject(DisponibilidadeApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(Auth);
  private carregamento?: Subscription;

  protected readonly servico = signal<Servico | null>(null);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<'nao-encontrado' | 'falha' | null>(null);
  protected readonly disponibilidades = signal<Disponibilidade[] | null>(null);
  protected readonly solicitando = signal(false);

  protected readonly ehMeuServico = computed(
    () => this.servico()?.prestador.usuarioId === this.auth.usuario()?.id,
  );

  protected readonly nomeCobranca = NOME_COBRANCA;
  protected readonly sufixoCobranca = SUFIXO_COBRANCA;

  constructor() {
    inject(DestroyRef).onDestroy(() => this.carregamento?.unsubscribe());
  }

  ngOnInit(): void {
    // Troca de serviço sem sair da página (ex.: link para outro /servicos/:id).
    this.route.paramMap.subscribe(() => this.carregar());
  }

  carregar(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.carregamento?.unsubscribe();
    this.servico.set(null);
    this.disponibilidades.set(null);
    this.solicitando.set(false);
    if (!Number.isInteger(id) || id <= 0) {
      this.erro.set('nao-encontrado');
      return;
    }
    this.carregando.set(true);
    this.erro.set(null);
    this.carregamento = this.servicoApi.buscar(id).subscribe({
      next: (servico) => {
        this.servico.set(servico);
        this.carregando.set(false);
        this.carregarDisponibilidade(servico.prestador.usuarioId);
        // Voltou do login com ?solicitar=1: abre o formulário direto.
        if (this.route.snapshot.queryParamMap.get('solicitar') && this.auth.estaLogado()) {
          this.solicitando.set(!this.ehMeuServico());
        }
      },
      error: (erro: unknown) => {
        this.erro.set(erro instanceof HttpErrorResponse && erro.status === 404 ? 'nao-encontrado' : 'falha');
        this.carregando.set(false);
      },
    });
  }

  // Quem não está logado vai para o login e volta para cá com o formulário aberto.
  solicitar(): void {
    const servico = this.servico();
    if (!servico) {
      return;
    }
    if (!this.auth.estaLogado()) {
      this.router.navigate(['/login'], {
        queryParams: { voltar: `/servicos/${servico.id}?solicitar=1` },
      });
      return;
    }
    this.solicitando.set(true);
  }

  // "Seg 08:00–18:00"
  descreverDisponibilidade(d: Disponibilidade): string {
    return `${NOME_DIA[d.diaSemana]} ${formatarHora(d.horaInicio)}–${formatarHora(d.horaFim)}`;
  }

  private carregarDisponibilidade(usuarioId: number): void {
    this.disponibilidadeApi.listar(usuarioId).subscribe({
      next: (lista) => this.disponibilidades.set(lista),
      error: () => this.disponibilidades.set(null),
    });
  }
}
