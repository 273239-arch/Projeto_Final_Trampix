import { FormControl, FormGroup } from '@angular/forms';
import {
  descreverPeriodo,
  formatarData,
  hojeEmBrasilia,
  quantidadeDias,
  validarPeriodo,
} from './periodo';

describe('periodo', () => {
  it('formata data sem mudar o dia pelo fuso', () => {
    expect(formatarData('2026-11-12')).toBe('12/11/2026');
  });

  it('conta os dias incluindo início e fim', () => {
    expect(quantidadeDias('2026-11-12', null)).toBe(1);
    expect(quantidadeDias('2026-11-12', '2026-11-14')).toBe(3);
    expect(quantidadeDias('2026-02-27', '2026-03-02')).toBe(4);
  });

  it('descreve período por hora e por dia', () => {
    expect(
      descreverPeriodo({ dataInicio: '2026-11-12', dataFim: '2026-11-12', horaInicio: '08:00:00', horaFim: '11:00:00' }),
    ).toBe('12/11/2026, 08:00 às 11:00');
    expect(
      descreverPeriodo({ dataInicio: '2026-11-12', dataFim: '2026-11-14', horaInicio: null, horaFim: null }),
    ).toBe('12/11/2026 a 14/11/2026');
  });

  it('valida data passada, fim antes do início e hora que passa da meia-noite', () => {
    const grupo = (porHora: boolean, valores: Record<string, unknown>) =>
      new FormGroup(
        {
          dataInicio: new FormControl(valores['dataInicio'] ?? ''),
          dataFim: new FormControl(valores['dataFim'] ?? ''),
          horaInicio: new FormControl(valores['horaInicio'] ?? ''),
          quantidadeHoras: new FormControl(valores['quantidadeHoras'] ?? null),
        },
        { validators: validarPeriodo(() => porHora) },
      );
    const hoje = hojeEmBrasilia();

    expect(grupo(false, { dataInicio: '2000-01-01' }).errors).toEqual({ dataPassada: true });
    expect(grupo(false, { dataInicio: '2999-01-10', dataFim: '2999-01-09' }).errors).toEqual({
      fimAntesDoInicio: true,
    });
    expect(grupo(true, { dataInicio: hoje, horaInicio: '22:00', quantidadeHoras: 3 }).errors).toEqual({
      passaDoDia: true,
    });
    expect(grupo(true, { dataInicio: '2999-01-10', horaInicio: '08:00', quantidadeHoras: 3 }).errors).toBeNull();
  });
});
