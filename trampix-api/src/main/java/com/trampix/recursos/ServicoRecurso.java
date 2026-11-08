package com.trampix.recursos;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.trampix.entidades.PrestadorPerfil;
import com.trampix.entidades.Servico;
import com.trampix.entidades.Subcategoria;
import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.Perfil;
import com.trampix.entidades.enums.StatusServico;
import com.trampix.repositorios.PrestadorPerfilRepositorio;
import com.trampix.repositorios.ServicoRepositorio;

import jakarta.validation.Valid;

// busca e CRUD de serviços do prestador
// TODO: filtros por cidade e preço
@RestController
@RequestMapping("/servicos")
public class ServicoRecurso {

    private final ServicoRepositorio repositorio;
    private final PrestadorPerfilRepositorio prestadorRepositorio;
    private final CriadorSubcategoria criadorSubcategoria;

    public ServicoRecurso(ServicoRepositorio repositorio, PrestadorPerfilRepositorio prestadorRepositorio,
            CriadorSubcategoria criadorSubcategoria) {
        this.repositorio = repositorio;
        this.prestadorRepositorio = prestadorRepositorio;
        this.criadorSubcategoria = criadorSubcategoria;
    }

    // lista os serviços ativos (filtros opcionais por categoria e subcategoria)
    @GetMapping
    public List<ServicoResposta> listar(@RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long subcategoriaId) {
        return repositorio.filtrar(StatusServico.ATIVO, categoriaId, subcategoriaId).stream()
                .map(ServicoResposta::de).toList();
    }

    // serviços do prestador logado, ativos e inativos
    @GetMapping("/meus")
    @Transactional(readOnly = true)
    public List<ServicoResposta> meus(@AuthenticationPrincipal Usuario logado) {
        PrestadorPerfil prestador = prestadorDo(logado);
        return repositorio.findByPrestadorIdOrderById(prestador.getId()).stream()
                .map(ServicoResposta::de).toList();
    }

    @GetMapping("/{id}")
    public ServicoResposta buscar(@PathVariable Long id) {
        return repositorio.findById(id)
                .filter(s -> s.getStatus() == StatusServico.ATIVO)
                .map(ServicoResposta::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "serviço não encontrado"));
    }

    // prestador logado cadastra mais um serviço
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ServicoResposta cadastrar(@Valid @RequestBody ServicoRequisicao dados,
            @AuthenticationPrincipal Usuario logado) {
        PrestadorPerfil prestador = travar(prestadorDo(logado));
        Subcategoria subcategoria = criadorSubcategoria.paraServico(dados.subcategoriaId(), dados.categoriaId(),
                dados.novaSubcategoria(), logado);
        exigirSubcategoriaLivre(prestador, subcategoria, null);
        Servico servico = new Servico();
        servico.setPrestador(prestador);
        dados.aplicar(servico, subcategoria);
        return ServicoResposta.de(repositorio.save(servico));
    }

    @PutMapping("/{id}")
    @Transactional
    public ServicoResposta atualizar(@PathVariable Long id, @Valid @RequestBody ServicoRequisicao dados,
            @AuthenticationPrincipal Usuario logado) {
        Servico servico = buscarDoDonoOuAdmin(id, logado);
        if (servico.getStatus() == StatusServico.INATIVO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "serviço inativo não pode ser editado");
        }
        PrestadorPerfil prestador = travar(servico.getPrestador());
        Subcategoria subcategoria = criadorSubcategoria.paraServico(dados.subcategoriaId(), dados.categoriaId(),
                dados.novaSubcategoria(), logado);
        if (servico.getStatus() == StatusServico.ATIVO) {
            exigirSubcategoriaLivre(prestador, subcategoria, id);
        }
        dados.aplicar(servico, subcategoria);
        return ServicoResposta.de(repositorio.save(servico));
    }

    // exclusão lógica: o serviço fica INATIVO
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void excluir(@PathVariable Long id, @AuthenticationPrincipal Usuario logado) {
        Servico servico = buscarDoDonoOuAdmin(id, logado);
        servico.setStatus(StatusServico.INATIVO);
        repositorio.save(servico);
    }

    private PrestadorPerfil prestadorDo(Usuario logado) {
        return prestadorRepositorio.findByUsuarioId(logado.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "só prestador pode fazer isso; cadastre o primeiro serviço em POST /usuarios/{id}/prestador"));
    }

    private PrestadorPerfil travar(PrestadorPerfil prestador) {
        return prestadorRepositorio.travar(prestador.getId()).orElseThrow();
    }

    // busca o serviço e confere se é do logado ou se ele é ADMIN
    private Servico buscarDoDonoOuAdmin(Long id, Usuario logado) {
        Servico servico = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "serviço não encontrado"));
        boolean dono = servico.getPrestador().getUsuario().getId().equals(logado.getId());
        if (!dono && logado.getPerfil() != Perfil.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "só o dono do serviço ou um ADMIN pode fazer isso");
        }
        return servico;
    }

    // um serviço ativo por subcategoria em cada prestador
    private void exigirSubcategoriaLivre(PrestadorPerfil prestador, Subcategoria subcategoria, Long idIgnorado) {
        if (repositorio.existeNaSubcategoria(prestador.getId(), subcategoria.getId(), StatusServico.ATIVO,
                idIgnorado)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "você já tem um serviço ativo na subcategoria "
                    + subcategoria.getNome() + "; edite o serviço existente");
        }
    }
}
