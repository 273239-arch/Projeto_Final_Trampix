package com.trampix.recursos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.trampix.entidades.Contratacao;
import com.trampix.entidades.Servico;
import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.StatusContratacao;
import com.trampix.entidades.enums.TipoCobranca;

// dados da contratação devolvidos no JSON
public record ContratacaoResposta(
        Long id,
        StatusContratacao status,
        LocalDate dataInicio,
        LocalDate dataFim,
        LocalTime horaInicio,
        LocalTime horaFim,
        String observacao,
        BigDecimal precoCombinado,
        String mensagemContraproposta,
        LocalDateTime contrapropostaEm,
        String motivoRecusa,
        LocalDateTime expiraEm,
        ServicoResumo servico,
        ParteResumo cliente,
        ParteResumo prestador,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    public record ServicoResumo(Long id, String titulo, TipoCobranca tipoCobranca) {
    }

    public record ParteResumo(Long id, String nome, String cidade, String uf, String telefone) {
    }

    public static ContratacaoResposta de(Contratacao contratacao, Usuario logado) {
        Servico servico = contratacao.getServico();
        Usuario cliente = contratacao.getCliente();
        Usuario prestador = servico.getPrestador().getUsuario();
        boolean aceita = contratacao.getStatus() == StatusContratacao.ACEITA
                || contratacao.getStatus() == StatusContratacao.CONCLUIDA;
        boolean logadoEhCliente = logado.getId().equals(cliente.getId());
        boolean logadoEhPrestador = logado.getId().equals(prestador.getId());
        return new ContratacaoResposta(
                contratacao.getId(),
                contratacao.getStatus(),
                contratacao.getDataInicio(),
                contratacao.getDataFim(),
                contratacao.getHoraInicio(),
                contratacao.getHoraFim(),
                contratacao.getObservacao(),
                contratacao.getPrecoCombinado(),
                contratacao.getMensagemContraproposta(),
                contratacao.getContrapropostaEm(),
                contratacao.getMotivoRecusa(),
                expiraEm(contratacao),
                new ServicoResumo(servico.getId(), servico.getTitulo(), servico.getTipoCobranca()),
                parte(cliente, aceita && logadoEhPrestador),
                parte(prestador, aceita && logadoEhCliente),
                contratacao.getCriadoEm(),
                contratacao.getAtualizadoEm());
    }

    // data em que o pedido ou a contraproposta expira
    private static LocalDateTime expiraEm(Contratacao contratacao) {
        return switch (contratacao.getStatus()) {
            case PENDENTE -> contratacao.getCriadoEm().plus(ContratacaoRecurso.PRAZO_RESPOSTA);
            case CONTRAPROPOSTA -> contratacao.getContrapropostaEm().plus(ContratacaoRecurso.PRAZO_RESPOSTA);
            default -> null;
        };
    }

    private static ParteResumo parte(Usuario usuario, boolean mostrarTelefone) {
        return new ParteResumo(usuario.getId(), usuario.getNome(), usuario.getCidade(), usuario.getUf(),
                mostrarTelefone ? usuario.getTelefone() : null);
    }
}
