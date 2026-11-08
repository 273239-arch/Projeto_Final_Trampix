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
import com.trampix.recursos.ServicoResposta.Resumo;

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
        String motivoContraproposta,
        LocalDateTime contrapropostaEm,
        String motivoRecusa,
        LocalDateTime expiraEm,
        ServicoResumo servico,
        ParteResumo cliente,
        ParteResumo prestador,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    public record ServicoResumo(Long id, String titulo, TipoCobranca tipoCobranca, Resumo categoria, Resumo subcategoria) {
    }

    public record ParteResumo(Long id, String nome, String cidade, String uf, String telefone, Endereco endereco) {
    }

    public record Endereco(String logradouro, String numero, String complemento, String bairro, String cidade,
            String uf, String cep) {
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
                contratacao.getMotivoContraproposta(),
                contratacao.getContrapropostaEm(),
                contratacao.getMotivoRecusa(),
                expiraEm(contratacao),
                new ServicoResumo(servico.getId(), servico.getTitulo(), servico.getTipoCobranca(),
                        new Resumo(servico.getCategoria().getId(), servico.getCategoria().getNome()),
                        new Resumo(servico.getSubcategoria().getId(), servico.getSubcategoria().getNome())),
                parte(cliente, aceita && logadoEhPrestador, aceita && logadoEhPrestador),
                parte(prestador, aceita && logadoEhCliente, false),
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

    // telefone e endereço só aparecem quando liberados
    private static ParteResumo parte(Usuario usuario, boolean mostrarTelefone, boolean mostrarEndereco) {
        Endereco endereco = mostrarEndereco
                ? new Endereco(usuario.getLogradouro(), usuario.getNumero(), usuario.getComplemento(),
                        usuario.getBairro(), usuario.getCidade(), usuario.getUf(), usuario.getCep())
                : null;
        return new ParteResumo(usuario.getId(), usuario.getNome(), usuario.getCidade(), usuario.getUf(),
                mostrarTelefone ? usuario.getTelefone() : null, endereco);
    }
}
