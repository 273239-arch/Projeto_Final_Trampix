export type TipoCobranca = 'POR_HORA' | 'POR_DIA' | 'POR_M2' | 'POR_TRABALHO';
export type StatusServico = 'ATIVO' | 'PAUSADO' | 'REMOVIDO';

// JSON público de GET /servicos (ServicoResposta no back).
// O back ainda não tem foto de serviço: o cartão mostra uma imagem padrão.
export interface Servico {
  id: number;
  titulo: string;
  descricao?: string | null;
  preco: number;
  tipoCobranca: TipoCobranca;
  status: StatusServico;
  categoria: { id: number; nome: string; categoriaPaiId: number | null };
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
