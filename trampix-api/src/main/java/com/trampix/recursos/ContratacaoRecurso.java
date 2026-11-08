package com.trampix.recursos;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.trampix.entidades.Contratacao;
import com.trampix.entidades.ContratacaoHistorico;
import com.trampix.entidades.PrestadorPerfil;
import com.trampix.entidades.Servico;
import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.DiaSemana;
import com.trampix.entidades.enums.Perfil;
import com.trampix.entidades.enums.StatusContratacao;
import com.trampix.entidades.enums.StatusServico;
import com.trampix.entidades.enums.StatusUsuario;
import com.trampix.repositorios.ContratacaoHistoricoRepositorio;
import com.trampix.repositorios.ContratacaoRepositorio;
import com.trampix.repositorios.DisponibilidadeRepositorio;
import com.trampix.repositorios.PrestadorPerfilRepositorio;
import com.trampix.repositorios.ServicoRepositorio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// endpoints de contratação
@RestController
@RequestMapping("/contratacoes")
public class ContratacaoRecurso {

    // fuso horário de Brasília
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    // duração de cada atendimento
    public static final Duration DURACAO_ATENDIMENTO = Duration.ofHours(1);

    public record CriarRequisicao(
            @NotNull Long servicoId,
            @NotNull LocalDateTime dataHoraDesejada,
            @Size(max = 1000) String observacao) {
    }

    // resposta do histórico
    public record HistoricoResposta(Long id, StatusContratacao statusAnterior, StatusContratacao statusNovo,
            AutorResumo autor, LocalDateTime criadoEm) {

        public record AutorResumo(Long id, String nome) {
        }

        static HistoricoResposta de(ContratacaoHistorico h) {
            return new HistoricoResposta(h.getId(), h.getStatusAnterior(), h.getStatusNovo(),
                    new AutorResumo(h.getAutor().getId(), h.getAutor().getNome()), h.getCriadoEm());
        }
    }

    private final ContratacaoRepositorio repositorio;
    private final ContratacaoHistoricoRepositorio historicoRepositorio;
    private final ServicoRepositorio servicoRepositorio;
    private final DisponibilidadeRepositorio disponibilidadeRepositorio;
    private final PrestadorPerfilRepositorio prestadorRepositorio;

    public ContratacaoRecurso(ContratacaoRepositorio repositorio, ContratacaoHistoricoRepositorio historicoRepositorio,
            ServicoRepositorio servicoRepositorio, DisponibilidadeRepositorio disponibilidadeRepositorio,
            PrestadorPerfilRepositorio prestadorRepositorio) {
        this.repositorio = repositorio;
        this.historicoRepositorio = historicoRepositorio;
        this.servicoRepositorio = servicoRepositorio;
        this.disponibilidadeRepositorio = disponibilidadeRepositorio;
        this.prestadorRepositorio = prestadorRepositorio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ContratacaoResposta criar(@Valid @RequestBody CriarRequisicao requisicao,
            @AuthenticationPrincipal Usuario logado) {
        Servico servico = servicoRepositorio.findById(requisicao.servicoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "serviço não encontrado"));
        PrestadorPerfil prestador = servico.getPrestador();
        if (servico.getStatus() != StatusServico.ATIVO) {
            throw regra("este serviço não está disponível para contratação (status " + servico.getStatus() + ")");
        }
        if (logado.getStatus() != StatusUsuario.ATIVO) {
            throw regra("só usuário ATIVO pode contratar");
        }
        if (prestador.getUsuario().getStatus() != StatusUsuario.ATIVO) {
            throw regra("o prestador deste serviço não está ativo");
        }
        if (prestador.getUsuario().getId().equals(logado.getId())) {
            throw regra("você não pode contratar o seu próprio serviço");
        }
        LocalDateTime inicio = requisicao.dataHoraDesejada().withSecond(0).withNano(0);
        exigirFuturo(inicio);
        exigirDentroDaDisponibilidade(prestador, inicio);
        exigirSemConflito(prestador, inicio, 0L);

        Contratacao contratacao = new Contratacao();
        contratacao.setServico(servico);
        contratacao.setCliente(logado);
        contratacao.setStatus(StatusContratacao.PENDENTE);
        contratacao.setDataHoraDesejada(inicio);
        contratacao.setObservacao(requisicao.observacao());
        contratacao.setPrecoCombinado(servico.getPreco());
        repositorio.save(contratacao);
        historicoRepositorio.save(new ContratacaoHistorico(contratacao, logado, null, StatusContratacao.PENDENTE));
        return ContratacaoResposta.de(contratacao, logado);
    }

    // lista as contratações do usuário logado (ADMIN pode filtrar)
    @GetMapping
    public List<ContratacaoResposta> listar(@RequestParam(required = false) String papel,
            @RequestParam(required = false) Long usuarioId, @RequestParam(required = false) Long servicoId,
            @AuthenticationPrincipal Usuario logado) {
        boolean admin = logado.getPerfil() == Perfil.ADMIN;
        if (!admin && (usuarioId != null || servicoId != null)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "os filtros usuarioId e servicoId são só para ADMIN");
        }
        boolean filtroDeAdmin = admin && (usuarioId != null || servicoId != null);
        Long alvo = filtroDeAdmin ? usuarioId : logado.getId();
        boolean comoCliente = true;
        boolean comoPrestador = true;
        if (papel != null && !papel.isBlank()) {
            if (alvo == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "o filtro papel precisa de usuarioId");
            }
            switch (papel.trim().toLowerCase(Locale.ROOT)) {
                case "cliente" -> comoPrestador = false;
                case "prestador" -> comoCliente = false;
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "papel inválido: use cliente ou prestador");
            }
        }
        return repositorio.filtrar(alvo, comoCliente, comoPrestador, filtroDeAdmin ? servicoId : null).stream()
                .map(c -> ContratacaoResposta.de(c, logado)).toList();
    }

    @GetMapping("/{id}")
    public ContratacaoResposta buscar(@PathVariable Long id, @AuthenticationPrincipal Usuario logado) {
        Contratacao contratacao = buscarOu404(id);
        exigirParteOuAdmin(contratacao, logado);
        return ContratacaoResposta.de(contratacao, logado);
    }

    @GetMapping("/{id}/historico")
    public List<HistoricoResposta> historico(@PathVariable Long id, @AuthenticationPrincipal Usuario logado) {
        Contratacao contratacao = buscarOu404(id);
        exigirParteOuAdmin(contratacao, logado);
        return historicoRepositorio.findByContratacaoIdOrderByCriadoEmAscIdAsc(id).stream()
                .map(HistoricoResposta::de).toList();
    }

    @PostMapping("/{id}/aceitar")
    @Transactional
    public ContratacaoResposta aceitar(@PathVariable Long id, @AuthenticationPrincipal Usuario logado) {
        Contratacao contratacao = buscarOu404(id);
        exigirPrestador(contratacao, logado, "aceitar");
        exigirStatus(contratacao, "aceitar", Set.of(StatusContratacao.PENDENTE));
        // trava o prestador
        PrestadorPerfil prestador = prestadorRepositorio.travar(contratacao.getServico().getPrestador().getId())
                .orElseThrow();
        exigirFuturo(contratacao.getDataHoraDesejada());
        exigirSemConflito(prestador, contratacao.getDataHoraDesejada(), contratacao.getId());
        return mudarStatus(contratacao, StatusContratacao.ACEITA, logado);
    }

    @PostMapping("/{id}/recusar")
    @Transactional
    public ContratacaoResposta recusar(@PathVariable Long id, @AuthenticationPrincipal Usuario logado) {
        Contratacao contratacao = buscarOu404(id);
        exigirPrestador(contratacao, logado, "recusar");
        exigirStatus(contratacao, "recusar", Set.of(StatusContratacao.PENDENTE));
        return mudarStatus(contratacao, StatusContratacao.RECUSADA, logado);
    }

    @PostMapping("/{id}/cancelar")
    @Transactional
    public ContratacaoResposta cancelar(@PathVariable Long id, @AuthenticationPrincipal Usuario logado) {
        Contratacao contratacao = buscarOu404(id);
        if (!ehCliente(contratacao, logado) && !ehPrestador(contratacao, logado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "só o cliente ou o prestador podem cancelar esta contratação");
        }
        exigirStatus(contratacao, "cancelar", Set.of(StatusContratacao.PENDENTE, StatusContratacao.ACEITA));
        return mudarStatus(contratacao, StatusContratacao.CANCELADA, logado);
    }

    @PostMapping("/{id}/concluir")
    @Transactional
    public ContratacaoResposta concluir(@PathVariable Long id, @AuthenticationPrincipal Usuario logado) {
        Contratacao contratacao = buscarOu404(id);
        exigirPrestador(contratacao, logado, "concluir");
        exigirStatus(contratacao, "concluir", Set.of(StatusContratacao.ACEITA));
        return mudarStatus(contratacao, StatusContratacao.CONCLUIDA, logado);
    }


    private static void exigirFuturo(LocalDateTime dataHora) {
        if (!dataHora.isAfter(LocalDateTime.now(FUSO))) {
            throw regra("a data e hora desejadas precisam estar no futuro (horário de Brasília)");
        }
    }

    // confere se o horário cabe na disponibilidade
    private void exigirDentroDaDisponibilidade(PrestadorPerfil prestador, LocalDateTime inicio) {
        LocalDateTime fim = inicio.plus(DURACAO_ATENDIMENTO);
        DiaSemana dia = DiaSemana.de(inicio.getDayOfWeek());
        boolean cabe = fim.toLocalDate().equals(inicio.toLocalDate())
                && disponibilidadeRepositorio.findByPrestadorIdAndDiaSemana(prestador.getId(), dia).stream()
                        .anyMatch(d -> !inicio.toLocalTime().isBefore(d.getHoraInicio())
                                && !fim.toLocalTime().isAfter(d.getHoraFim()));
        if (!cabe) {
            throw regra("o prestador não atende neste horário: o atendimento (" + DURACAO_ATENDIMENTO.toHours()
                    + "h a partir de " + inicio.toLocalTime() + ") precisa caber numa disponibilidade de " + dia);
        }
    }

    // confere se já tem contratação aceita no horário
    private void exigirSemConflito(PrestadorPerfil prestador, LocalDateTime inicio, Long idIgnorado) {
        boolean conflito = repositorio.existeNoIntervalo(prestador.getId(), StatusContratacao.ACEITA,
                inicio.minus(DURACAO_ATENDIMENTO), inicio.plus(DURACAO_ATENDIMENTO), idIgnorado);
        if (conflito) {
            throw regra("o prestador já tem uma contratação aceita que conflita com este horário");
        }
    }

    private static void exigirPrestador(Contratacao contratacao, Usuario logado, String acao) {
        if (!ehPrestador(contratacao, logado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "só o prestador do serviço pode " + acao);
        }
    }

    private static void exigirStatus(Contratacao contratacao, String acao, Set<StatusContratacao> permitidos) {
        if (!permitidos.contains(contratacao.getStatus())) {
            throw regra("não é possível " + acao + " uma contratação " + contratacao.getStatus()
                    + ": só é permitido a partir de " + String.join(" ou ",
                            permitidos.stream().map(Enum::name).sorted().toList()));
        }
    }

    private static void exigirParteOuAdmin(Contratacao contratacao, Usuario logado) {
        boolean parte = ehCliente(contratacao, logado) || ehPrestador(contratacao, logado);
        if (!parte && logado.getPerfil() != Perfil.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "só o cliente, o prestador ou um ADMIN podem ver esta contratação");
        }
    }

    // muda o status e salva no histórico
    private ContratacaoResposta mudarStatus(Contratacao contratacao, StatusContratacao novo, Usuario logado) {
        historicoRepositorio.save(new ContratacaoHistorico(contratacao, logado, contratacao.getStatus(), novo));
        contratacao.setStatus(novo);
        return ContratacaoResposta.de(repositorio.saveAndFlush(contratacao), logado);
    }

    private static boolean ehCliente(Contratacao contratacao, Usuario logado) {
        return contratacao.getCliente().getId().equals(logado.getId());
    }

    private static boolean ehPrestador(Contratacao contratacao, Usuario logado) {
        return contratacao.getServico().getPrestador().getUsuario().getId().equals(logado.getId());
    }

    private Contratacao buscarOu404(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "contratação não encontrada"));
    }

    private static ResponseStatusException regra(String mensagem) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, mensagem);
    }
}
