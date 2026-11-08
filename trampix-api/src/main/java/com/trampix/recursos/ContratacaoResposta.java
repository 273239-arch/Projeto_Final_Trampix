package com.trampix.recursos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.trampix.entidades.Contratacao;
import com.trampix.entidades.Servico;
import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.StatusContratacao;
import com.trampix.entidades.enums.TipoCobranca;

// dados da contratação devolvidos no JSON
public record ContratacaoResposta(
        Long id,
        StatusContratacao status,
        LocalDateTime dataHoraDesejada,
        String observacao,
        BigDecimal precoCombinado,
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
                contratacao.getDataHoraDesejada(),
                contratacao.getObservacao(),
                contratacao.getPrecoCombinado(),
                new ServicoResumo(servico.getId(), servico.getTitulo(), servico.getTipoCobranca()),
                parte(cliente, aceita && logadoEhPrestador),
                parte(prestador, aceita && logadoEhCliente),
                contratacao.getCriadoEm(),
                contratacao.getAtualizadoEm());
    }

    private static ParteResumo parte(Usuario usuario, boolean mostrarTelefone) {
        return new ParteResumo(usuario.getId(), usuario.getNome(), usuario.getCidade(), usuario.getUf(),
                mostrarTelefone ? usuario.getTelefone() : null);
    }
}
