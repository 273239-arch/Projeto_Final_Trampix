import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { ErroApi } from '../../core/models/erro-api';
import { NovoUsuario, TipoDocumento } from '../../core/models/usuario';
import { Auth } from '../../core/services/auth';
import { UsuarioApi } from '../../core/services/usuario-api';

type Campo =
  | 'nome'
  | 'email'
  | 'senha'
  | 'confirmarSenha'
  | 'tipoDocumento'
  | 'documento'
  | 'telefone'
  | 'dataNascimento'
  | 'aceiteDocumentos'
  | 'aceiteTermos';

// Mensagens dos validadores do front (o erro "servidor" traz o texto do back).
const MENSAGENS: Record<string, string> = {
  required: 'Campo obrigatório.',
  email: 'E-mail inválido.',
  maxlength: 'Texto muito longo.',
  senhaTamanho: 'A senha deve ter entre 8 e 72 caracteres.',
  senhasDiferentes: 'As senhas não conferem.',
  cpf: 'O CPF deve ter 11 dígitos.',
  cnpj: 'O CNPJ deve ter 14 dígitos.',
  telefone: 'Informe DDD + número (10 ou 11 dígitos).',
  dataFutura: 'A data deve ser no passado.',
};

const TAMANHO_MAXIMO_ARQUIVO = 10 * 1024 * 1024; // 10 MB

interface Arquivo {
  nome?: string;
  erro?: string;
}

@Component({
  selector: 'app-cadastro',
  standalone: false,
  styleUrl: './cadastro.scss',
  templateUrl: './cadastro.html',
})
export class Cadastro {
  private readonly usuarioApi = inject(UsuarioApi);
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly enviando = signal(false);
  protected readonly erroGeral = signal<string | null>(null);
  protected readonly mostrarSenha = signal(false);
  protected readonly documento = signal<Arquivo>({});
  protected readonly selfie = signal<Arquivo>({});

  protected readonly form = inject(FormBuilder).nonNullable.group(
    {
      nome: ['', [Validators.required, Validators.maxLength(120)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(160)]],
      senha: ['', [Validators.required, tamanhoSenha]],
      confirmarSenha: ['', Validators.required],
      tipoDocumento: ['CPF' as TipoDocumento, Validators.required],
      documento: ['', [Validators.required, documentoValido]],
      telefone: ['', [Validators.required, telefoneValido]],
      dataNascimento: ['', dataPassada],
      // Só no front: não vão para o back.
      aceiteDocumentos: [false, Validators.requiredTrue],
      aceiteTermos: [false, Validators.requiredTrue],
    },
    { validators: senhasIguais },
  );

  constructor() {
    // Trocar CPF/CNPJ muda a regra do documento.
    this.form.controls.tipoDocumento.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.form.controls.documento.updateValueAndValidity());
  }

  // Primeira mensagem de erro do campo, para mostrar embaixo dele.
  erro(campo: Campo): string | null {
    const controle = this.form.controls[campo];
    const erros = controle.errors;
    if (!erros || !(controle.touched || controle.dirty)) {
      return null;
    }
    if (erros['servidor']) {
      return erros['servidor'] as string;
    }
    const chave = Object.keys(erros)[0];
    return MENSAGENS[chave] ?? 'Valor inválido.';
  }

  criarConta(): void {
    this.erroGeral.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valores = this.form.getRawValue();
    const usuario: NovoUsuario = {
      nome: valores.nome.trim(),
      email: valores.email.trim(),
      senha: valores.senha,
      tipoDocumento: valores.tipoDocumento,
      documento: soDigitos(valores.documento),
      telefone: soDigitos(valores.telefone),
      dataNascimento: valores.dataNascimento || null,
    };

    this.enviando.set(true);
    this.usuarioApi.cadastrar(usuario).subscribe({
      next: () => this.entrarDepoisDoCadastro(usuario),
      error: (erro: HttpErrorResponse) => {
        this.enviando.set(false);
        this.mostrarErrosDoBack(erro);
      },
    });
  }

  escolherArquivo(tipo: 'documento' | 'selfie', evento: Event): void {
    const input = evento.target as HTMLInputElement;
    this.guardarArquivo(tipo, input.files?.[0]);
  }

  soltarArquivo(tipo: 'documento' | 'selfie', evento: DragEvent): void {
    evento.preventDefault();
    this.guardarArquivo(tipo, evento.dataTransfer?.files?.[0]);
  }

  // O back ainda não recebe arquivos: aqui só conferimos tipo e tamanho e mostramos o nome.
  private guardarArquivo(tipo: 'documento' | 'selfie', arquivo?: File): void {
    if (!arquivo) {
      return;
    }
    const aceitos =
      tipo === 'documento'
        ? ['image/jpeg', 'image/png', 'application/pdf']
        : ['image/jpeg', 'image/png'];
    const estado = tipo === 'documento' ? this.documento : this.selfie;
    if (!aceitos.includes(arquivo.type)) {
      estado.set({ erro: 'Formato não aceito.' });
    } else if (arquivo.size > TAMANHO_MAXIMO_ARQUIVO) {
      estado.set({ erro: 'O arquivo passa de 10 MB.' });
    } else {
      estado.set({ nome: arquivo.name });
    }
  }

  // Depois de criar a conta, já faz o login com o mesmo e-mail e senha.
  private entrarDepoisDoCadastro(usuario: NovoUsuario): void {
    this.auth.login({ email: usuario.email, senha: usuario.senha }).subscribe({
      next: () => {
        this.enviando.set(false);
        this.snackBar.open('Conta criada! Bem-vindo(a) ao Trampix.', 'Fechar', { duration: 4000 });
        this.router.navigate(['/']);
      },
      error: () => {
        this.enviando.set(false);
        this.router.navigate(['/login']);
      },
    });
  }

  // 400 com "erros": cada mensagem vai para o seu campo. 409 (e-mail ou documento repetido)
  // não traz campo, então olhamos o texto. O snackbar do interceptor já mostrou a mensagem geral.
  private mostrarErrosDoBack(erro: HttpErrorResponse): void {
    const corpo = erro.error as Partial<ErroApi> | null;
    if (!corpo?.mensagem) {
      return;
    }
    const naoAtribuidos: string[] = [];
    for (const item of corpo.erros ?? []) {
      if (!this.marcarErro(item.campo, item.mensagem)) {
        naoAtribuidos.push(item.mensagem);
      }
    }
    if (erro.status === 409) {
      const campo = /e-mail/i.test(corpo.mensagem)
        ? 'email'
        : /documento/i.test(corpo.mensagem)
          ? 'documento'
          : null;
      if (!campo || !this.marcarErro(campo, corpo.mensagem)) {
        naoAtribuidos.push(corpo.mensagem);
      }
    }
    if (naoAtribuidos.length) {
      this.erroGeral.set(naoAtribuidos.join(' '));
    }
  }

  private marcarErro(campo: string, mensagem: string): boolean {
    const controle = this.form.get(campo);
    if (!controle) {
      return false;
    }
    const texto = mensagem.charAt(0).toUpperCase() + mensagem.slice(1);
    controle.setErrors({ ...controle.errors, servidor: texto });
    controle.markAsTouched();
    return true;
  }
}

function soDigitos(texto: string): string {
  return texto.replace(/\D/g, '');
}

const tamanhoSenha: ValidatorFn = (c) => {
  const tamanho = (c.value as string).length;
  return tamanho && (tamanho < 8 || tamanho > 72) ? { senhaTamanho: true } : null;
};

// CPF: 11 dígitos. CNPJ: 14. Os dígitos verificadores o back confere e devolve o erro no campo.
const documentoValido: ValidatorFn = (c) => {
  const digitos = soDigitos(c.value as string);
  if (!digitos) {
    return null;
  }
  const tipo = c.parent?.get('tipoDocumento')?.value as TipoDocumento | undefined;
  if (tipo === 'CNPJ') {
    return digitos.length === 14 ? null : { cnpj: true };
  }
  return digitos.length === 11 ? null : { cpf: true };
};

const telefoneValido: ValidatorFn = (c) => {
  const digitos = soDigitos(c.value as string);
  return !digitos || digitos.length === 10 || digitos.length === 11 ? null : { telefone: true };
};

const dataPassada: ValidatorFn = (c) => {
  const valor = c.value as string; // yyyy-MM-dd do <input type="date">
  const agora = new Date();
  const hoje = [
    agora.getFullYear(),
    String(agora.getMonth() + 1).padStart(2, '0'),
    String(agora.getDate()).padStart(2, '0'),
  ].join('-');
  return valor && valor >= hoje ? { dataFutura: true } : null;
};

// Erro de grupo, mas é colocado no "confirmar senha" para aparecer embaixo dele.
function senhasIguais(grupo: AbstractControl): ValidationErrors | null {
  const confirmar = grupo.get('confirmarSenha');
  if (!confirmar) {
    return null;
  }
  const diferentes = confirmar.value && grupo.get('senha')?.value !== confirmar.value;
  const { senhasDiferentes: _, ...outros } = confirmar.errors ?? {};
  if (diferentes) {
    confirmar.setErrors({ ...outros, senhasDiferentes: true });
  } else if (confirmar.hasError('senhasDiferentes')) {
    confirmar.setErrors(Object.keys(outros).length ? outros : null);
  }
  return null;
}
