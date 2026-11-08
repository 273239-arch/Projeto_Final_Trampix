import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { Auth } from '../../core/services/auth';

@Component({
  selector: 'app-login',
  standalone: false,
  styleUrl: './login.scss',
  templateUrl: './login.html',
})
export class Login {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly entrando = signal(false);
  protected readonly mostrarSenha = signal(false);

  // O Figma pede "Nome de usuário", mas o back faz login por e-mail.
  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', Validators.required],
  });

  entrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, senha } = this.form.getRawValue();
    this.entrando.set(true);
    // Erros (401 "e-mail ou senha inválidos", 403 conta suspensa...) aparecem no snackbar do interceptor.
    this.auth.login({ email: email.trim(), senha }).subscribe({
      next: (resposta) => {
        this.entrando.set(false);
        this.snackBar.open(`Olá, ${resposta.usuario.nome}!`, 'Fechar', { duration: 3000 });
        this.router.navigateByUrl(this.destino());
      },
      error: () => this.entrando.set(false),
    });
  }

  esqueciSenha(): void {
    this.snackBar.open('A recuperação de senha ainda não está disponível.', 'Fechar', {
      duration: 4000,
    });
  }

  // Volta para a página que pediu login (?voltar=/...), só se for um caminho interno.
  private destino(): string {
    const voltar = this.route.snapshot.queryParamMap.get('voltar');
    return voltar?.startsWith('/') && !voltar.startsWith('//') ? voltar : '/';
  }
}
