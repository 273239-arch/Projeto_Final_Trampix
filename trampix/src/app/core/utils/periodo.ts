import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

// Datas como o back usa: "yyyy-MM-dd" (LocalDate) e "HH:mm" (LocalTime), no horário de Brasília.

// Hoje em Brasília, no formato "yyyy-MM-dd" (o mesmo do <input type="date">).
export function hojeEmBrasilia(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo' }).format(new Date());
}

// "2026-11-12" -> "12/11/2026", sem passar por new Date() (que trocaria o dia pelo fuso).
export function formatarData(data: string | null | undefined): string {
  if (!data) {
    return '';
  }
  const [ano, mes, dia] = data.slice(0, 10).split('-');
  return `${dia}/${mes}/${ano}`;
}

// "08:00:00" -> "08:00".
export function formatarHora(hora: string | null | undefined): string {
  return hora ? hora.slice(0, 5) : '';
}

// Dias entre as duas datas, contando as duas pontas (como o back).
export function quantidadeDias(inicio: string, fim: string | null | undefined): number {
  const dia = (texto: string) => {
    const [a, m, d] = texto.split('-').map(Number);
    return Date.UTC(a, m - 1, d);
  };
  return Math.round((dia(fim || inicio) - dia(inicio)) / 86_400_000) + 1;
}

// Minutos desde a meia-noite de "HH:mm".
export function minutos(hora: string): number {
  const [h, m] = hora.split(':').map(Number);
  return h * 60 + m;
}

// Texto do período: "12/11/2026, 08:00 às 11:00" ou "12/11/2026 a 14/11/2026".
export function descreverPeriodo(p: {
  dataInicio: string;
  dataFim: string | null;
  horaInicio: string | null;
  horaFim: string | null;
}): string {
  if (p.horaInicio) {
    return `${formatarData(p.dataInicio)}, ${formatarHora(p.horaInicio)} às ${formatarHora(p.horaFim)}`;
  }
  if (!p.dataFim || p.dataFim === p.dataInicio) {
    return formatarData(p.dataInicio);
  }
  return `${formatarData(p.dataInicio)} a ${formatarData(p.dataFim)}`;
}

// Validador do grupo de datas. Por hora: data, horaInicio e quantidadeHoras, terminando no mesmo dia.
// Outros tipos: dataInicio e dataFim opcional, que não pode ser antes do início.
export function validarPeriodo(porHora: () => boolean): ValidatorFn {
  return (grupo: AbstractControl): ValidationErrors | null => {
    const { dataInicio, dataFim, horaInicio, quantidadeHoras } = grupo.value as {
      dataInicio?: string;
      dataFim?: string;
      horaInicio?: string;
      quantidadeHoras?: number | null;
    };
    if (!dataInicio) {
      return null;
    }
    if (dataInicio < hojeEmBrasilia()) {
      return { dataPassada: true };
    }
    if (porHora()) {
      if (horaInicio && quantidadeHoras && minutos(horaInicio) + quantidadeHoras * 60 > 24 * 60) {
        return { passaDoDia: true };
      }
      return null;
    }
    if (dataFim && dataFim < dataInicio) {
      return { fimAntesDoInicio: true };
    }
    return null;
  };
}

// Mensagens dos erros do validarPeriodo.
export const ERROS_PERIODO: Record<string, string> = {
  dataPassada: 'A data de início precisa ser hoje ou no futuro.',
  passaDoDia: 'O atendimento por hora precisa terminar no mesmo dia.',
  fimAntesDoInicio: 'A data de fim não pode ser antes da data de início.',
};
