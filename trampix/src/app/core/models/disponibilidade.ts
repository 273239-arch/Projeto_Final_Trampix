export type DiaSemana = 'SEGUNDA' | 'TERCA' | 'QUARTA' | 'QUINTA' | 'SEXTA' | 'SABADO' | 'DOMINGO';

export const NOME_DIA: Record<DiaSemana, string> = {
  SEGUNDA: 'Seg',
  TERCA: 'Ter',
  QUARTA: 'Qua',
  QUINTA: 'Qui',
  SEXTA: 'Sex',
  SABADO: 'Sáb',
  DOMINGO: 'Dom',
};

// DisponibilidadeResposta no back. Horas "HH:mm:ss".
export interface Disponibilidade {
  id: number;
  diaSemana: DiaSemana;
  horaInicio: string;
  horaFim: string;
}
