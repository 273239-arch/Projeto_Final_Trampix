import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Contraproposta, Contratacao } from '../../core/models/contratacao';
import { ContratacaoApi } from '../../core/services/contratacao-api';
import { aplicarErrosDoBack, mensagemDoCampo } from '../../core/utils/erros-formulario';
import {
  ERROS_PERIODO,
  formatarHora,
  hojeEmBrasilia,
  minutos,
  validarPeriodo,
} from '../../core/utils/periodo';

type Campo = 'dataInicio' | 'dataFim' | 'horaInicio' | 'quantidadeHoras' | 'precoCombinado' | 'motivo';

const MENSAGENS: Record<string, string> = {
  required: 'Campo obrigatório.',
  min: 'Valor abaixo do mínimo.',
  max: 'Máximo de 24 horas.',
  minlength: 'O motivo deve ter entre 10 e 500 caracteres.',
  maxlength: 'O motivo deve ter entre 10 e 500 caracteres.',
};

// Contraproposta do prestador (POST /contratacoes/{id}/contraproposta): novas datas ou horas,
// preço (opcional, senão o back calcula) e motivo obrigatório de 10 a 500 caracteres.
@Component({
  selector: 'app-form-contraproposta',
  standalone: false,
  styleUrl: './form-contraproposta.scss',
  templateUrl: './form-contraproposta.html',
})
export class FormContraproposta implements OnInit {
  private readonly contratacaoApi = inject(ContratacaoApi);
  private readonly snackBar = inject(MatSnackBar);

  readonly pedido = input.required<Contratacao>();
  readonly enviada = output<Contratacao>();
  readonly fechar = output<void>();

  protected readonly enviando = signal(false);
  protected readonly erroGeral = signal<string | null>(null);
  protected readonly hoje = hojeEmBrasilia();
  protected readonly porHora = computed(() => this.pedido().servico.tipoCobranca === 'POR_HORA');

  protected readonly form = inject(FormBuilder).group(
    {
      dataInicio: ['', Validators.required],
      dataFim: [''],
      horaInicio: [''],
      quantidadeHoras: [null as number | null, [Validators.min(1), Validators.max(24)]],
      precoCombinado: [null as number | null, Validators.min(0.01)],
      motivo: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]],
    },
    { validators: validarPeriodo(() => this.porHora()) },
  );

  private readonly valores = toSignal(this.form.valueChanges, { initialValue: this.form.value });
  protected readonly tamanhoMotivo = computed(() => (this.valores().motivo ?? '').trim().length);

  ngOnInit(): void {
    // Começa com as datas do pedido, para o prestador só ajustar.
    const p = this.pedido();
    if (this.porHora()) {
      this.form.controls.horaInicio.addValidators(Validators.required);
      this.form.controls.quantidadeHoras.addValidators(Validators.required);
    }
    this.form.patchValue({
      dataInicio: p.dataInicio,
      dataFim: p.dataFim !== p.dataInicio ? p.dataFim : '',
      horaInicio: formatarHora(p.horaInicio),
      quantidadeHoras:
        p.horaInicio && p.horaFim ? (minutos(p.horaFim) - minutos(p.horaInicio)) / 60 : null,
    });
  }

  erro(campo: Campo): string {
    return mensagemDoCampo(this.form.controls[campo], MENSAGENS);
  }

  erroPeriodo(): string {
    const erros = this.form.errors;
    if (!erros || !(this.form.controls.dataInicio.touched || this.form.controls.dataFim.touched)) {
      return '';
    }
    return ERROS_PERIODO[Object.keys(erros)[0]] ?? '';
  }

  enviar(): void {
    this.erroGeral.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const proposta: Contraproposta = {
      dataInicio: v.dataInicio!,
      motivo: v.motivo!.trim(),
      precoCombinado: v.precoCombinado ? Number(v.precoCombinado) : null,
    };
    if (this.porHora()) {
      proposta.horaInicio = v.horaInicio;
      proposta.quantidadeHoras = Number(v.quantidadeHoras);
    } else {
      proposta.dataFim = v.dataFim || null;
    }
    this.enviando.set(true);
    this.contratacaoApi.contrapropor(this.pedido().id, proposta).subscribe({
      next: (atualizado) => {
        this.enviando.set(false);
        this.snackBar.open('Contraproposta enviada ao cliente.', 'Fechar', { duration: 4000 });
        this.enviada.emit(atualizado);
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erroGeral.set(aplicarErrosDoBack(this.form, erro));
      },
    });
  }
}
