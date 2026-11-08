export type TipoCobranca = 'POR_HORA' | 'POR_DIA' | 'POR_M2' | 'POR_TRABALHO';
export type StatusServico = 'ATIVO' | 'PAUSADO' | 'REMOVIDO';

// Figma: "R$ 70,00 / hora".
export const SUFIXO_COBRANCA: Record<TipoCobranca, string> = {
  POR_HORA: '/ hora',
  POR_DIA: '/ dia',
  POR_M2: '/ m²',
  POR_TRABALHO: '/ trabalho',
};

// Nome do tipo de cobrança por extenso, para o detalhe do serviço.
export const NOME_COBRANCA: Record<TipoCobranca, string> = {
  POR_HORA: 'Por hora',
  POR_DIA: 'Por dia',
  POR_M2: 'Por m²',
  POR_TRABALHO: 'Por trabalho completo',
};

// JSON público de GET /servicos (ServicoResposta no back).
// O back ainda não tem foto de serviço: o cartão mostra uma imagem padrão.
export interface Servico {
  id: number;
  titulo: string;
  descricao?: string | null;
  preco: number;
  tipoCobranca: TipoCobranca;
  status: StatusServico;
  categoria: { id: number; nome: string };
  subcategoria: { id: number; nome: string };
  prestador: {
    id: number;
    usuarioId: number;
    nome: string;
    cidade: string | null;
    uf: string | null;
    verificado: boolean;
  };
  criadoEm: string;
}
