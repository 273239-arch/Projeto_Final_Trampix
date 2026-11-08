import { HttpClient } from '@angular/common/http';
import { computed, inject, Service, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequisicao, LoginResposta } from '../models/login-resposta';
import { Usuario } from '../models/usuario';

const CHAVE_SESSAO = 'trampix.sessao';

interface Sessao {
  token: string;
  expiraEm: string;
  usuario: Usuario;
}

// Guarda o token JWT do login e diz se há alguém logado.
@Service()
export class Auth {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/auth/login`;
  private readonly sessao = signal<Sessao | null>(lerSessao());

  readonly usuario = computed(() => this.sessao()?.usuario ?? null);

  login(dados: LoginRequisicao): Observable<LoginResposta> {
    return this.http.post<LoginResposta>(this.url, dados).pipe(
      tap((resposta) =>
        this.salvar({ token: resposta.token, expiraEm: resposta.expiraEm, usuario: resposta.usuario }),
      ),
    );
  }

  logout(): void {
    this.sessao.set(null);
    try {
      localStorage.removeItem(CHAVE_SESSAO);
    } catch {
      // localStorage indisponível (modo privado, por exemplo): nada a limpar.
    }
  }

  // Devolve o token só se ainda não expirou. Token vencido é descartado aqui.
  token(): string | null {
    const sessao = this.sessao();
    if (!sessao) {
      return null;
    }
    if (!dentroDaValidade(sessao)) {
      this.logout();
      return null;
    }
    return sessao.token;
  }

  estaLogado(): boolean {
    return this.token() !== null;
  }

  private salvar(sessao: Sessao): void {
    this.sessao.set(sessao);
    try {
      localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao));
    } catch {
      // Sem localStorage a sessão vale só até recarregar a página.
    }
  }
}

function dentroDaValidade(sessao: Sessao): boolean {
  return new Date(sessao.expiraEm).getTime() > Date.now();
}

function lerSessao(): Sessao | null {
  try {
    const texto = localStorage.getItem(CHAVE_SESSAO);
    if (!texto) {
      return null;
    }
    const sessao = JSON.parse(texto) as Sessao;
    return dentroDaValidade(sessao) ? sessao : null;
  } catch {
    return null;
  }
}
