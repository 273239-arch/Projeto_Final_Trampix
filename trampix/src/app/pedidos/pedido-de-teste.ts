import { Contratacao } from '../core/models/contratacao';

// Só para os testes: um pedido completo, com os campos que o teste quiser trocar.
export function pedidoDeTeste(dados: Partial<Contratacao>): Contratacao {
  return {
    id: 1,
    status: 'PENDENTE',
    dataInicio: '2999-01-10',
    dataFim: '2999-01-10',
    horaInicio: null,
    horaFim: null,
    observacao: null,
    precoCombinado: 300,
    motivoContraproposta: null,
    contrapropostaEm: null,
    motivoRecusa: null,
    expiraEm: null,
    servico: { id: 7, titulo: 'Pintura de sala', tipoCobranca: 'POR_DIA' },
    cliente: { id: 2, nome: 'Maria', cidade: 'Olinda', uf: 'PE', telefone: null },
    prestador: { id: 9, nome: 'José', cidade: 'Recife', uf: 'PE', telefone: null },
    criadoEm: '2026-11-01T10:00:00',
    atualizadoEm: '2026-11-01T10:00:00',
    ...dados,
  };
}
