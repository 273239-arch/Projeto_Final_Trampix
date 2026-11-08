package com.trampix.recursos;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.trampix.entidades.Disponibilidade;
import com.trampix.entidades.Servico;
import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.DiaSemana;
import com.trampix.entidades.enums.Perfil;
import com.trampix.entidades.enums.StatusContratacao;
import com.trampix.entidades.enums.StatusServico;
import com.trampix.entidades.enums.StatusUsuario;
import com.trampix.entidades.enums.TipoCobranca;
import com.trampix.repositorios.ContratacaoHistoricoRepositorio;
import com.trampix.repositorios.ContratacaoRepositorio;
import com.trampix.repositorios.DisponibilidadeRepositorio;
import com.trampix.repositorios.PrestadorPerfilRepositorio;
import com.trampix.repositorios.ServicoRepositorio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// endpoints de contratação
@RestController
@RequestMapping("/contratacoes")
public class ContratacaoRecurso {

    // fuso horário de Brasília
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    // prazo para responder um pedido ou uma contraproposta
    public static final Duration PRAZO_RESPOSTA = Duration.ofDays(7);

    public record CriarRequisicao(
            @NotNull Long servicoId,
            @NotNull LocalDate dataInicio,
            LocalDate dataFim,
            LocalTime horaInicio,
            @Min(1) @Max(24) Integer quantidadeHoras,
            @Size(max = 1000) String observacao) {
    }

    public record ContrapropostaRequisicao(
            @NotNull LocalDate dataInicio,
            LocalDate dataFim,
            LocalTime horaInicio,
            @Min(1) @Max(24) Integer quantidadeHoras,
            @DecimalMin(value = "0.01", message = "o preço deve ser maior que zero")
            @Digits(integer = 8, fraction = 2) BigDecimal precoCombinado,
            @NotBlank @Size(min = 10, max = 500, message = "o motivo deve ter entre 10 e 500 caracteres") String motivo) {
    }

    public record RecusaRequisicao(@Size(max = 500) String motivo) {
    }

    // resposta do histórico
    public record HistoricoResposta(Long id, StatusContratacao statusAnterior, StatusContratacao statusNovo,
            AutorResumo autor, LocalDate dataInicio, LocalDate dataFim, LocalTime horaInicio, LocalTime horaFim,
            BigDecimal precoCombinado, Integer diasContados, String motivo, LocalDateTime criadoEm) {

        public record AutorResumo(Long id, String nome) {
        }

        static HistoricoResposta de(ContratacaoHistorico h) {
            AutorResumo autor = h.getAutor() == null ? null
                    : new AutorResumo(h.getAutor().getId(), h.getAutor().getNome());
            return new HistoricoResposta(h.getId(), h.getStatusAnterior(), h.getStatusNovo(), autor,
                    h.getDataInicio(), h.getDataFim(), h.getHoraInicio(), h.getHoraFim(), h.getPrecoCombinado(),
                    h.getDiasContados(), h.getMotivo(), h.getCriadoEm());
        }
    }

    // datas e horas de um pedido (horas nulas quando é por dia)
    record Periodo(LocalDate dataInicio, LocalDate dataFim, LocalTime horaInicio, LocalTime horaFim) {

        boolean porHora() {
            return horaInicio != null;
        }

        long horas() {
            return Duration.between(horaInicio, horaFim).toHours();
        }
    }

    private final ContratacaoRepositorio repositorio;
    private final ContratacaoHistoricoRepositorio historicoRepositorio;
    private final ServicoRepositorio servicoRepositorio;
    private final DisponibilidadeRepositorio disponibilidadeRepositorio;
    private final PrestadorPerfilRepositorio prestadorRepositorio;
    private final ExpiradorContratacao expirador;

    public ContratacaoRecurso(ContratacaoRepositorio repositorio, ContratacaoHistoricoRepositorio historicoRepositorio,
            ServicoRepositorio servicoRepositorio, DisponibilidadeRepositorio disponibilidadeRepositorio,
            PrestadorPerfilRepositorio prestadorRepositorio, ExpiradorContratacao expirador) {
        this.expirador = expirador;
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
        if (servico.getStatus() != StatusServico.ATIVO) {
            throw regra("este serviço não está disponível para contratação (status " + servico.getStatus() + ")");
        }
        if (logado.getStatus() != StatusUsuario.ATIVO) {
            throw regra("só usuário ATIVO pode contratar");
        }
        Usuario prestador = servico.getPrestador().getUsuario();
        if (prestador.getStatus() != StatusUsuario.ATIVO) {
            throw regra("o prestador deste serviço não está ativo");
        }
        if (prestador.getId().equals(logado.getId())) {
            throw regra("você não pode contratar o seu próprio serviço");
        }
        Periodo periodo = montarPeriodo(servico, requisicao.dataInicio(), requisicao.dataFim(),
                requisicao.horaInicio(), requisicao.quantidadeHoras());
        exigirDataValida(periodo);
        int dias = diasAtendidos(servico, periodo);
        exigirSemConflito(servico, periodo, 0L);

        Contratacao contratacao = new Contratacao();
        contratacao.setServico(servico);
        contratacao.setCliente(logado);
        contratacao.setStatus(StatusContratacao.PENDENTE);
        aplicarPeriodo(contratacao, periodo);
        contratacao.setObservacao(requisicao.observacao());
        contratacao.setDiasContados(dias);
        contratacao.setPrecoCombinado(calcularPreco(servico, periodo, dias));
        repositorio.save(contratacao);
        historicoRepositorio.save(new ContratacaoHistorico(contratacao, logado, null, null));
        return ContratacaoResposta.de(contratacao, logado);
    }

    // lista as contratações do usuário logado (ADMIN pode filtrar)
    @GetMapping
    @Transactional
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
        expirador.expirarVencidas();
        return repositorio.filtrar(alvo, comoCliente, comoPrestador, filtroDeAdmin ? servicoId : null).stream()
                .map(c -> ContratacaoResposta.de(c, logado)).toList();
    }

    @GetMapping("/{id}")
    @Transactional
    public ContratacaoResposta buscar(@PathVariable Long id, @AuthenticationPrincipal Usuario logado) {
        expirador.expirarVencidas();
        Contratacao contratacao = buscarOu404(id);
        exigirParteOuAdmin(contratacao, logado);
        return ContratacaoResposta.de(contratacao, logado);
    }

    @GetMapping("/{id}/historico")
    @Transactional
    public List<HistoricoResposta> historico(@PathVariable Long id, @AuthenticationPrincipal Usuario logado) {
        expirador.expirarVencidas();
        Contratacao contratacao = buscarOu404(id);
        exigirParteOuAdmin(contratacao, logado);
        return historicoRepositorio.findByContratacaoIdOrderByCriadoEmAscIdAsc(id).stream()
                .map(HistoricoResposta::de).toList();
    }

    @PostMapping("/{id}/aceitar")
    @Transactional
    public ContratacaoResposta aceitar(@PathVariable Long id, @AuthenticationPrincipal Usuario logado) {
        expirador.expirarVencidas();
        Contratacao contratacao = buscarOu404(id);
        exigirPrestador(contratacao, logado, "aceitar");
        exigirStatus(contratacao, "aceitar", EnumSet.of(StatusContratacao.PENDENTE));
        travarPrestador(contratacao);
        Periodo periodo = periodoDe(contratacao);
        exigirDataValida(periodo);
        exigirSemConflito(contratacao.getServico(), periodo, contratacao.getId());
        return mudarStatus(contratacao, StatusContratacao.ACEITA, logado, null);
    }

    @PostMapping("/{id}/contraproposta")
    @Transactional
    public ContratacaoResposta contrapropor(@PathVariable Long id,
            @Valid @RequestBody ContrapropostaRequisicao requisicao, @AuthenticationPrincipal Usuario logado) {
        expirador.expirarVencidas();
        Contratacao contratacao = buscarOu404(id);
        exigirPrestador(contratacao, logado, "fazer contraproposta");
        if (contratacao.getContrapropostaEm() != null) {
            throw regra("o prestador já fez uma contraproposta neste pedido");
        }
        exigirStatus(contratacao, "fazer contraproposta em", EnumSet.of(StatusContratacao.PENDENTE));
        Servico servico = contratacao.getServico();
        Periodo periodo = montarPeriodo(servico, requisicao.dataInicio(), requisicao.dataFim(),
                requisicao.horaInicio(), requisicao.quantidadeHoras());
        exigirDataValida(periodo);
        int dias = diasAtendidos(servico, periodo);
        exigirSemConflito(servico, periodo, contratacao.getId());

        aplicarPeriodo(contratacao, periodo);
        contratacao.setDiasContados(dias);
        contratacao.setPrecoCombinado(requisicao.precoCombinado() != null
                ? requisicao.precoCombinado() : calcularPreco(servico, periodo, dias));
        contratacao.setMotivoContraproposta(requisicao.motivo());
        contratacao.setContrapropostaEm(LocalDateTime.now());
        return mudarStatus(contratacao, StatusContratacao.CONTRAPROPOSTA, logado, requisicao.motivo());
    }

    @PostMapping("/{id}/aceitar-contraproposta")
    @Transactional
    public ContratacaoResposta aceitarContraproposta(@PathVariable Long id, @AuthenticationPrincipal Usuario logado) {
        expirador.expirarVencidas();
        Contratacao contratacao = buscarOu404(id);
        if (!ehCliente(contratacao, logado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "só o cliente pode aceitar a contraproposta");
        }
        exigirStatus(contratacao, "aceitar a contraproposta de", EnumSet.of(StatusContratacao.CONTRAPROPOSTA));
        travarPrestador(contratacao);
        Periodo periodo = periodoDe(contratacao);
        exigirDataValida(periodo);
        diasAtendidos(contratacao.getServico(), periodo);
        exigirSemConflito(contratacao.getServico(), periodo, contratacao.getId());
        return mudarStatus(contratacao, StatusContratacao.ACEITA, logado, null);
    }

    // prestador recusa o pedido ou cliente recusa a contraproposta
    @PostMapping("/{id}/recusar")
    @Transactional
    public ContratacaoResposta recusar(@PathVariable Long id,
            @Valid @RequestBody(required = false) RecusaRequisicao requisicao, @AuthenticationPrincipal Usuario logado) {
        expirador.expirarVencidas();
        Contratacao contratacao = buscarOu404(id);
        if (ehPrestador(contratacao, logado)) {
            exigirStatus(contratacao, "recusar (como prestador)", EnumSet.of(StatusContratacao.PENDENTE));
        } else if (ehCliente(contratacao, logado)) {
            exigirStatus(contratacao, "recusar (como cliente)", EnumSet.of(StatusContratacao.CONTRAPROPOSTA));
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "só o cliente ou o prestador podem recusar esta contratação");
        }
        String motivo = requisicao == null ? null : requisicao.motivo();
        contratacao.setMotivoRecusa(motivo);
        return mudarStatus(contratacao, StatusContratacao.RECUSADA, logado, motivo);
    }

    @PostMapping("/{id}/cancelar")
    @Transactional
    public ContratacaoResposta cancelar(@PathVariable Long id, @AuthenticationPrincipal Usuario logado) {
        expirador.expirarVencidas();
        Contratacao contratacao = buscarOu404(id);
        if (!ehCliente(contratacao, logado) && !ehPrestador(contratacao, logado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "só o cliente ou o prestador podem cancelar esta contratação");
        }
        exigirStatus(contratacao, "cancelar", EnumSet.of(StatusContratacao.PENDENTE,
                StatusContratacao.CONTRAPROPOSTA, StatusContratacao.ACEITA));
        return mudarStatus(contratacao, StatusContratacao.CANCELADA, logado, null);
    }

    @PostMapping("/{id}/concluir")
    @Transactional
    public ContratacaoResposta concluir(@PathVariable Long id, @AuthenticationPrincipal Usuario logado) {
        expirador.expirarVencidas();
        Contratacao contratacao = buscarOu404(id);
        exigirPrestador(contratacao, logado, "concluir");
        exigirStatus(contratacao, "concluir", EnumSet.of(StatusContratacao.ACEITA));
        if (LocalDate.now(FUSO).isBefore(contratacao.getDataInicio())) {
            throw regra("a contratação só pode ser concluída a partir da data de início (" + contratacao.getDataInicio() + ")");
        }
        return mudarStatus(contratacao, StatusContratacao.CONCLUIDA, logado, null);
    }

    // monta as datas e horas conforme o tipo de cobrança do serviço
    private static Periodo montarPeriodo(Servico servico, LocalDate dataInicio, LocalDate dataFim,
            LocalTime horaInicio, Integer quantidadeHoras) {
        if (servico.getTipoCobranca() == TipoCobranca.POR_HORA) {
            if (horaInicio == null || quantidadeHoras == null) {
                throw invalido("serviço cobrado por hora: informe horaInicio e quantidadeHoras");
            }
            if (dataFim != null && !dataFim.equals(dataInicio)) {
                throw invalido("serviço cobrado por hora: o pedido é de um dia só (não informe outra dataFim)");
            }
            LocalDateTime inicio = dataInicio.atTime(horaInicio.truncatedTo(ChronoUnit.MINUTES));
            LocalDateTime fim = inicio.plusHours(quantidadeHoras);
            if (!fim.toLocalDate().equals(dataInicio)) {
                throw regra("o atendimento por hora precisa terminar no mesmo dia");
            }
            return new Periodo(dataInicio, dataInicio, inicio.toLocalTime(), fim.toLocalTime());
        }
        if (horaInicio != null || quantidadeHoras != null) {
            throw invalido("serviço cobrado por " + servico.getTipoCobranca()
                    + ": informe só dataInicio e dataFim, sem horaInicio e quantidadeHoras");
        }
        LocalDate fim = dataFim == null ? dataInicio : dataFim;
        if (fim.isBefore(dataInicio)) {
            throw regra("a dataFim não pode ser antes da dataInicio");
        }
        return new Periodo(dataInicio, fim, null, null);
    }

    private static Periodo periodoDe(Contratacao c) {
        return new Periodo(c.getDataInicio(), c.getDataFim(), c.getHoraInicio(), c.getHoraFim());
    }

    private static void aplicarPeriodo(Contratacao c, Periodo periodo) {
        c.setDataInicio(periodo.dataInicio());
        c.setDataFim(periodo.dataFim());
        c.setHoraInicio(periodo.horaInicio());
        c.setHoraFim(periodo.horaFim());
    }

    // calcula o preço pelo tipo de cobrança
    private static BigDecimal calcularPreco(Servico servico, Periodo periodo, int dias) {
        return switch (servico.getTipoCobranca()) {
            case POR_HORA -> servico.getPreco().multiply(BigDecimal.valueOf(periodo.horas()));
            case POR_DIA -> servico.getPreco().multiply(BigDecimal.valueOf(dias));
            case POR_TRABALHO, POR_M2 -> servico.getPreco();
        };
    }

    // confere se a data é hoje ou no futuro
    private static void exigirDataValida(Periodo periodo) {
        LocalDateTime agora = LocalDateTime.now(FUSO);
        if (periodo.dataInicio().isBefore(agora.toLocalDate())) {
            throw regra("a dataInicio precisa ser hoje ou no futuro (horário de Brasília)");
        }
        if (periodo.porHora() && !periodo.dataInicio().atTime(periodo.horaInicio()).isAfter(agora)) {
            throw regra("o horário de início já passou (horário de Brasília)");
        }
    }

    // confere a disponibilidade e devolve quantos dias do período o prestador atende
    private int diasAtendidos(Servico servico, Periodo periodo) {
        List<Disponibilidade> janelas = disponibilidadeRepositorio.findByPrestadorId(servico.getPrestador().getId());
        if (periodo.porHora()) {
            DiaSemana dia = DiaSemana.de(periodo.dataInicio().getDayOfWeek());
            boolean cabe = janelas.stream().anyMatch(d -> d.getDiaSemana() == dia
                    && !periodo.horaInicio().isBefore(d.getHoraInicio())
                    && !periodo.horaFim().isAfter(d.getHoraFim()));
            if (!cabe) {
                throw regra("o prestador não atende neste horário: " + periodo.horaInicio() + "-"
                        + periodo.horaFim() + " precisa caber numa disponibilidade de " + dia);
            }
            return 1;
        }
        Set<DiaSemana> diasComJanela = janelas.stream().map(Disponibilidade::getDiaSemana)
                .collect(Collectors.toSet());
        // dias sem disponibilidade não contam
        int dias = 0;
        for (LocalDate data = periodo.dataInicio(); !data.isAfter(periodo.dataFim()); data = data.plusDays(1)) {
            if (diasComJanela.contains(DiaSemana.de(data.getDayOfWeek()))) {
                dias++;
            }
        }
        if (dias == 0) {
            throw regra("o prestador não atende em nenhum dia do período de " + periodo.dataInicio() + " a "
                    + periodo.dataFim() + "; escolha datas em que ele tenha disponibilidade");
        }
        return dias;
    }

    // confere se já tem contratação aceita que cruza com o pedido
    private void exigirSemConflito(Servico servico, Periodo periodo, Long idIgnorado) {
        List<Contratacao> aceitas = repositorio.doPrestadorNasDatas(servico.getPrestador().getId(),
                StatusContratacao.ACEITA, periodo.dataInicio(), periodo.dataFim(), idIgnorado);
        for (Contratacao outra : aceitas) {
            boolean conflita = !periodo.porHora() || outra.getHoraInicio() == null
                    || (periodo.horaInicio().isBefore(outra.getHoraFim())
                            && outra.getHoraInicio().isBefore(periodo.horaFim()));
            if (conflita) {
                throw regra("o prestador já tem uma contratação aceita que conflita com estas datas (contratação "
                        + outra.getId() + ")");
            }
        }
    }


    // trava o prestador
    private void travarPrestador(Contratacao contratacao) {
        prestadorRepositorio.travar(contratacao.getServico().getPrestador().getId()).orElseThrow();
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
                            permitidos.stream().map(Enum::name).toList()));
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
    private ContratacaoResposta mudarStatus(Contratacao contratacao, StatusContratacao novo, Usuario logado,
            String motivo) {
        StatusContratacao anterior = contratacao.getStatus();
        contratacao.setStatus(novo);
        repositorio.saveAndFlush(contratacao);
        historicoRepositorio.save(new ContratacaoHistorico(contratacao, logado, anterior, motivo));
        return ContratacaoResposta.de(contratacao, logado);
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

    private static ResponseStatusException invalido(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
