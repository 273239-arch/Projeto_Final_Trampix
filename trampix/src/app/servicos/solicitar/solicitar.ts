import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { NovaContratacao } from '../../core/models/contratacao';
import { Disponibilidade, NOME_DIA } from '../../core/models/disponibilidade';
import { Servico } from '../../core/models/servico';
import { ContratacaoApi } from '../../core/services/contratacao-api';
import { aplicarErrosDoBack, mensagemDoCampo } from '../../core/utils/erros-formulario';
import {
  ERROS_PERIODO,
  formatarHora,
  hojeEmBrasilia,
  quantidadeDias,
  validarPeriodo,
} from '../../core/utils/periodo';

type Campo = 'dataInicio' | 'dataFim' | 'horaInicio' | 'quantidadeHoras' | 'observacao';

const MENSAGENS: Record<string, string> = {
  required: 'Campo obrigatório.',
  min: 'Mínimo de 1 hora.',
  max: 'Máximo de 24 horas.',
  maxlength: 'Máximo de 1000 caracteres.',
};

// Formulário "Solicitar serviço" (POST /contratacoes). Serviço POR_HORA pede data, hora de início e
// quantidade de horas; os outros pedem data de início e data de fim (opcional).
@Component({
  selector: 'app-solicitar',
  standalone: false,
  styleUrl: './solicitar.scss',
  templateUrl: './solicitar.html',
})
export class Solicitar implements OnInit {
  private readonly contratacaoApi = inject(ContratacaoApi);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly servico = input.required<Servico>();
  readonly disponibilidades = input<Disponibilidade[] | null>(null);
  readonly fechar = output<void>();

  protected readonly enviando = signal(false);
  protected readonly erroGeral = signal<string | null>(null);
  protected readonly hoje = hojeEmBrasilia();
  protected readonly porHora = computed(() => this.servico().tipoCobranca === 'POR_HORA');

  protected readonly form = inject(FormBuilder).group(
    {
      dataInicio: ['', Validators.required],
      dataFim: [''],
      horaInicio: [''],
      quantidadeHoras: [null as number | null, [Validators.min(1), Validators.max(24)]],
      observacao: ['', Validators.maxLength(1000)],
    },
    { validators: validarPeriodo(() => this.porHora()) },
  );

  private readonly valores = toSignal(this.form.valueChanges, { initialValue: this.form.value });

  // Dias da semana em que o prestador atende, para ajudar a escolher a data.
  protected readonly diasQueAtende = computed(() =>
    (this.disponibilidades() ?? [])
      .map((d) => `${NOME_DIA[d.diaSemana]} ${formatarHora(d.horaInicio)}–${formatarHora(d.horaFim)}`)
      .join(', '),
  );

  // Preço x horas (POR_HORA) ou x dias (POR_DIA). Por m² e por trabalho, o preço do serviço.
  protected readonly estimativa = computed(() => {
    const servico = this.servico();
    const { dataInicio, dataFim, quantidadeHoras } = this.valores();
    switch (servico.tipoCobranca) {
      case 'POR_HORA': {
        const horas = Number(quantidadeHoras);
        if (!Number.isInteger(horas) || horas < 1 || horas > 24) {
          return null;
        }
        return { total: servico.preco * horas, conta: `${horas} ${horas === 1 ? 'hora' : 'horas'}` };
      }
      case 'POR_DIA': {
        if (!dataInicio || (dataFim && dataFim < dataInicio)) {
          return null;
        }
        const dias = quantidadeDias(dataInicio, dataFim);
        return { total: servico.preco * dias, conta: `${dias} ${dias === 1 ? 'dia' : 'dias'}` };
      }
      default:
        return { total: servico.preco, conta: null };
    }
  });

  protected readonly tamanhoObservacao = computed(() => (this.valores().observacao ?? '').length);

  ngOnInit(): void {
    // Os campos de hora só são obrigatórios para serviço cobrado por hora.
    if (this.porHora()) {
      this.form.controls.horaInicio.addValidators(Validators.required);
      this.form.controls.quantidadeHoras.addValidators(Validators.required);
    }
  }

  erro(campo: Campo): string {
    return mensagemDoCampo(this.form.controls[campo], MENSAGENS);
  }

  // Erro do grupo (data no passado, fim antes do início, passa da meia-noite).
  erroPeriodo(): string {
    const erros = this.form.errors;
    const mexeu = this.form.controls.dataInicio.touched || this.form.controls.dataFim.touched;
    if (!erros || !mexeu) {
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
    const pedido: NovaContratacao = {
      servicoId: this.servico().id,
      dataInicio: v.dataInicio!,
      observacao: v.observacao?.trim() || null,
    };
    if (this.porHora()) {
      pedido.horaInicio = v.horaInicio;
      pedido.quantidadeHoras = Number(v.quantidadeHoras);
    } else {
      pedido.dataFim = v.dataFim || null;
    }
    this.enviando.set(true);
    this.contratacaoApi.criar(pedido).subscribe({
      next: () => {
        this.enviando.set(false);
        this.snackBar.open('Pedido enviado! Agora é aguardar a resposta do prestador.', 'Fechar', {
          duration: 5000,
        });
        this.router.navigate(['/pedidos']);
      },
      error: (erro: unknown) => {
        this.enviando.set(false);
        this.erroGeral.set(aplicarErrosDoBack(this.form, erro));
      },
    });
  }
}
