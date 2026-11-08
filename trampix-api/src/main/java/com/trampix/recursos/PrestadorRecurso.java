package com.trampix.recursos;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.trampix.entidades.PrestadorPerfil;
import com.trampix.entidades.Servico;
import com.trampix.entidades.Subcategoria;
import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.StatusUsuario;
import com.trampix.repositorios.PrestadorPerfilRepositorio;
import com.trampix.repositorios.ServicoRepositorio;
import com.trampix.repositorios.UsuarioRepositorio;
import com.trampix.seguranca.Permissoes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// cria o perfil de prestador com o primeiro serviço
@RestController
@RequestMapping("/usuarios/{usuarioId}/prestador")
public class PrestadorRecurso {

    // dados do perfil e do primeiro serviço
    public record TornarPrestadorRequisicao(
            @Size(max = 1000) String biografia,
            @Min(0) Integer raioAtendimentoKm,
            @NotNull @Valid ServicoRequisicao servico) {
    }

    private final UsuarioRepositorio usuarioRepositorio;
    private final PrestadorPerfilRepositorio prestadorRepositorio;
    private final CriadorSubcategoria criadorSubcategoria;
    private final ServicoRepositorio servicoRepositorio;

    public PrestadorRecurso(UsuarioRepositorio usuarioRepositorio, PrestadorPerfilRepositorio prestadorRepositorio,
            CriadorSubcategoria criadorSubcategoria, ServicoRepositorio servicoRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.prestadorRepositorio = prestadorRepositorio;
        this.criadorSubcategoria = criadorSubcategoria;
        this.servicoRepositorio = servicoRepositorio;
    }

    @GetMapping
    public PrestadorPerfil buscar(@PathVariable Long usuarioId, @AuthenticationPrincipal Usuario logado) {
        Permissoes.exigirProprioOuAdmin(logado, usuarioId);
        return prestadorRepositorio.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "este usuário não é prestador"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ServicoResposta tornarPrestador(@PathVariable Long usuarioId,
            @Valid @RequestBody TornarPrestadorRequisicao requisicao, @AuthenticationPrincipal Usuario logado) {
        Permissoes.exigirProprioOuAdmin(logado, usuarioId);
        Usuario usuario = usuarioRepositorio.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "usuário não encontrado"));
        if (usuario.getStatus() != StatusUsuario.ATIVO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "só usuário ATIVO pode virar prestador");
        }
        if (prestadorRepositorio.existsByUsuarioId(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "este usuário já é prestador");
        }
        ServicoRequisicao dados = requisicao.servico();
        Subcategoria subcategoria = criadorSubcategoria.paraServico(dados.subcategoriaId(), dados.categoriaId(),
                dados.novaSubcategoria(), logado);

        PrestadorPerfil prestador = new PrestadorPerfil();
        prestador.setUsuario(usuario);
        prestador.setBiografia(requisicao.biografia());
        prestador.setRaioAtendimentoKm(requisicao.raioAtendimentoKm());
        prestadorRepositorio.save(prestador);

        Servico servico = new Servico();
        servico.setPrestador(prestador);
        dados.aplicar(servico, subcategoria);
        return ServicoResposta.de(servicoRepositorio.save(servico));
    }
}
