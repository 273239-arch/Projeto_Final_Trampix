import { TipoCobranca } from './servico';

export type StatusContratacao =
  | 'PENDENTE'
  | 'CONTRAPROPOSTA'
  | 'ACEITA'
  | 'RECUSADA'
  | 'CONCLUIDA'
  | 'CANCELADA'
  | 'EXPIRADA';

export const NOME_STATUS: Record<StatusContratacao, string> = {
  PENDENTE: 'Pendente',
  CONTRAPROPOSTA: 'Contraproposta',
  ACEITA: 'Aceita',
  RECUSADA: 'Recusada',
  CONCLUIDA: 'Concluída',
  CANCELADA: 'Cancelada',
  EXPIRADA: 'Expirada',
};

// Endereço do cliente, só para o prestador e só em ACEITA ou CONCLUIDA.
export interface Endereco {
  logradouro: string | null;
  numero: string | null;
  complemento: string | null;
  bairro: string | null;
  cidade: string | null;
  uf: string | null;
  cep: string | null;
}

// Cliente ou prestador. O telefone só vem depois de aceita (e só para a outra parte).
export interface ParteResumo {
  id: number;
  nome: string;
  cidade: string | null;
  uf: string | null;
  telefone: string | null;
  endereco: Endereco | null;
}

// Resumo do serviço na contratação, já com categoria e subcategoria.
export interface ServicoResumo {
  id: number;
  titulo: string;
  tipoCobranca: TipoCobranca;
  categoria: { id: number; nome: string };
  subcategoria: { id: number; nome: string };
}

// ContratacaoResposta no back. Datas "yyyy-MM-dd", horas "HH:mm:ss" (nulas quando não é por hora).
export interface Contratacao {
  id: number;
  status: StatusContratacao;
  dataInicio: string;
  dataFim: string;
  horaInicio: string | null;
  horaFim: string | null;
  observacao: string | null;
  precoCombinado: number;
  motivoContraproposta: string | null;
  contrapropostaEm: string | null;
  motivoRecusa: string | null;
  expiraEm: string | null;
  servico: ServicoResumo;
  cliente: ParteResumo;
  prestador: ParteResumo;
  criadoEm: string;
  atualizadoEm: string;
}

// Corpo do POST /contratacoes. Por hora: horaInicio + quantidadeHoras. Outros: dataFim opcional.
export interface NovaContratacao {
  servicoId: number;
  dataInicio: string;
  dataFim?: string | null;
  horaInicio?: string | null;
  quantidadeHoras?: number | null;
  observacao?: string | null;
}

// Corpo do POST /contratacoes/{id}/contraproposta. Sem preço, o back calcula pelo serviço.
export interface Contraproposta {
  dataInicio: string;
  dataFim?: string | null;
  horaInicio?: string | null;
  quantidadeHoras?: number | null;
  precoCombinado?: number | null;
  motivo: string;
}

export type Papel = 'cliente' | 'prestador';
