import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { filter, map } from 'rxjs';
import { Auth } from '../../core/services/auth';

// App Shell: cabeçalho (logo, busca, usuário), conteúdo da página e rodapé.
@Component({
  selector: 'app-layout',
  standalone: false,
  styleUrl: './layout.scss',
  templateUrl: './layout.html',
})
export class Layout {
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly auth = inject(Auth);
  protected readonly ano = new Date().getFullYear();

  // Mantém o texto da busca no campo quando a página é aberta com ?busca=.
  protected readonly busca = toSignal(
    inject(ActivatedRoute).queryParamMap.pipe(map((p) => p.get('busca') ?? '')),
    { initialValue: '' },
  );

  // No Figma, a tela "Criar sua conta" tem só o logo no cabeçalho.
  protected readonly cabecalhoSimples = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map(() => this.ehCadastro()),
    ),
    { initialValue: this.ehCadastro() },
  );

  // A busca é feita na home, que filtra os serviços pelo texto.
  buscar(evento: Event, texto: string): void {
    evento.preventDefault();
    const busca = texto.trim();
    this.router.navigate(['/'], { queryParams: busca ? { busca } : {} });
  }

  sair(): void {
    this.auth.logout();
    this.snackBar.open('Você saiu da conta.', 'Fechar', { duration: 3000 });
    this.router.navigate(['/']);
  }

  private ehCadastro(): boolean {
    return this.router.url.startsWith('/clientes/cadastro');
  }
}
