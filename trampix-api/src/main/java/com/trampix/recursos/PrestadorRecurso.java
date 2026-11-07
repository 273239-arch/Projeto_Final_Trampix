package com.trampix.recursos;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.trampix.entidades.Categoria;
import com.trampix.entidades.PrestadorPerfil;
import com.trampix.entidades.Servico;
import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.StatusUsuario;
import com.trampix.entidades.enums.TipoCobranca;
import com.trampix.repositorios.CategoriaRepositorio;
import com.trampix.repositorios.PrestadorPerfilRepositorio;
import com.trampix.repositorios.ServicoRepositorio;
import com.trampix.repositorios.UsuarioRepositorio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Decisão: o usuário vira prestador ao cadastrar o primeiro serviço.
// Este recurso cria, numa só transação, o prestador_perfil e esse primeiro serviço.
// TODO(segurança): com JWT, usar o usuário logado em vez do {usuarioId} da URL.
@RestController
@RequestMapping("/usuarios/{usuarioId}/prestador")
public class PrestadorRecurso {

    // Corpo do POST: dados do perfil de prestador + o primeiro serviço.
    public record TornarPrestadorRequisicao(
            @Size(max = 1000) String biografia,
            @Min(0) Integer raioAtendimentoKm,
            @NotNull @Valid PrimeiroServico servico) {
    }

    public record PrimeiroServico(
            @NotNull Long categoriaId,
            @NotBlank @Size(max = 120) String titulo,
            @NotBlank @Size(max = 2000) String descricao,
            @NotNull @DecimalMin(value = "0.01", message = "o preço deve ser maior que zero")
            @Digits(integer = 8, fraction = 2) BigDecimal preco,
            @NotNull TipoCobranca tipoCobranca) {
    }

    private final UsuarioRepositorio usuarioRepositorio;
    private final PrestadorPerfilRepositorio prestadorRepositorio;
    private final CategoriaRepositorio categoriaRepositorio;
    private final ServicoRepositorio servicoRepositorio;

    public PrestadorRecurso(UsuarioRepositorio usuarioRepositorio, PrestadorPerfilRepositorio prestadorRepositorio,
            CategoriaRepositorio categoriaRepositorio, ServicoRepositorio servicoRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.prestadorRepositorio = prestadorRepositorio;
        this.categoriaRepositorio = categoriaRepositorio;
        this.servicoRepositorio = servicoRepositorio;
    }

    @GetMapping
    public PrestadorPerfil buscar(@PathVariable Long usuarioId) {
        return prestadorRepositorio.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "este usuário não é prestador"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Servico tornarPrestador(@PathVariable Long usuarioId, @Valid @RequestBody TornarPrestadorRequisicao requisicao) {
        Usuario usuario = usuarioRepositorio.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "usuário não encontrado"));
        if (usuario.getStatus() != StatusUsuario.ATIVO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "só usuário ATIVO pode virar prestador");
        }
        if (prestadorRepositorio.existsByUsuarioId(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "este usuário já é prestador");
        }
        PrimeiroServico dados = requisicao.servico();
        Categoria categoria = categoriaRepositorio.findById(dados.categoriaId())
                .filter(Categoria::isAtiva)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoria não encontrada"));

        PrestadorPerfil prestador = new PrestadorPerfil();
        prestador.setUsuario(usuario);
        prestador.setBiografia(requisicao.biografia());
        prestador.setRaioAtendimentoKm(requisicao.raioAtendimentoKm());
        prestadorRepositorio.save(prestador);

        Servico servico = new Servico();
        servico.setPrestador(prestador);
        servico.setCategoria(categoria);
        servico.setTitulo(dados.titulo());
        servico.setDescricao(dados.descricao());
        servico.setPreco(dados.preco());
        servico.setTipoCobranca(dados.tipoCobranca());
        return servicoRepositorio.save(servico);
    }
}
