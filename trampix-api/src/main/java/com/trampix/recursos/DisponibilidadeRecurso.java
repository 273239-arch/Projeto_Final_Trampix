package com.trampix.recursos;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.trampix.entidades.Disponibilidade;
import com.trampix.entidades.PrestadorPerfil;
import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.DiaSemana;
import com.trampix.repositorios.DisponibilidadeRepositorio;
import com.trampix.repositorios.PrestadorPerfilRepositorio;
import com.trampix.repositorios.UsuarioRepositorio;
import com.trampix.seguranca.Permissoes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

// endpoints de disponibilidade do prestador
@RestController
@RequestMapping("/usuarios/{usuarioId}/disponibilidades")
public class DisponibilidadeRecurso {

    // corpo do PUT
    public record SubstituirRequisicao(@NotNull List<@NotNull @Valid Item> disponibilidades) {
    }

    public record Item(@NotNull DiaSemana diaSemana, @NotNull LocalTime horaInicio, @NotNull LocalTime horaFim) {

        @AssertTrue(message = "horaFim deve ser maior que horaInicio")
        public boolean isHoraFimDepoisDoInicio() {
            return horaInicio == null || horaFim == null || horaFim.isAfter(horaInicio);
        }
    }

    public record DisponibilidadeResposta(Long id, DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFim) {

        static DisponibilidadeResposta de(Disponibilidade d) {
            return new DisponibilidadeResposta(d.getId(), d.getDiaSemana(), d.getHoraInicio(), d.getHoraFim());
        }
    }

    // ordena por dia e horário
    private static final Comparator<Disponibilidade> ORDEM = Comparator
            .comparing(Disponibilidade::getDiaSemana)
            .thenComparing(Disponibilidade::getHoraInicio);

    private final UsuarioRepositorio usuarioRepositorio;
    private final PrestadorPerfilRepositorio prestadorRepositorio;
    private final DisponibilidadeRepositorio repositorio;

    public DisponibilidadeRecurso(UsuarioRepositorio usuarioRepositorio,
            PrestadorPerfilRepositorio prestadorRepositorio, DisponibilidadeRepositorio repositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.prestadorRepositorio = prestadorRepositorio;
        this.repositorio = repositorio;
    }

    @GetMapping
    public List<DisponibilidadeResposta> listar(@PathVariable Long usuarioId) {
        if (!usuarioRepositorio.existsById(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "usuário não encontrado");
        }
        PrestadorPerfil prestador = prestadorRepositorio.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "este usuário não é prestador"));
        return repositorio.findByPrestadorId(prestador.getId()).stream()
                .sorted(ORDEM).map(DisponibilidadeResposta::de).toList();
    }

    @PutMapping
    @Transactional
    public List<DisponibilidadeResposta> substituir(@PathVariable Long usuarioId,
            @Valid @RequestBody SubstituirRequisicao requisicao, @AuthenticationPrincipal Usuario logado) {
        Permissoes.exigirProprioOuAdmin(logado, usuarioId);
        if (!usuarioRepositorio.existsById(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "usuário não encontrado");
        }
        PrestadorPerfil prestador = prestadorRepositorio.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                        "só prestador tem disponibilidade: cadastre um serviço primeiro"));

        List<Disponibilidade> novas = requisicao.disponibilidades().stream().map(item -> {
            Disponibilidade d = new Disponibilidade();
            d.setPrestador(prestador);
            d.setDiaSemana(item.diaSemana());
            d.setHoraInicio(item.horaInicio());
            d.setHoraFim(item.horaFim());
            return d;
        }).sorted(ORDEM).toList();
        recusarSobreposicao(novas);

        repositorio.apagarDoPrestador(prestador.getId());
        return repositorio.saveAll(novas).stream().map(DisponibilidadeResposta::de).toList();
    }

    // verifica horários sobrepostos no mesmo dia
    private static void recusarSobreposicao(List<Disponibilidade> ordenadas) {
        for (int i = 1; i < ordenadas.size(); i++) {
            Disponibilidade anterior = ordenadas.get(i - 1);
            Disponibilidade atual = ordenadas.get(i);
            if (anterior.getDiaSemana() == atual.getDiaSemana()
                    && atual.getHoraInicio().isBefore(anterior.getHoraFim())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, String.format(
                        "horários sobrepostos em %s: %s-%s e %s-%s", atual.getDiaSemana(),
                        anterior.getHoraInicio(), anterior.getHoraFim(), atual.getHoraInicio(), atual.getHoraFim()));
            }
        }
    }
}
