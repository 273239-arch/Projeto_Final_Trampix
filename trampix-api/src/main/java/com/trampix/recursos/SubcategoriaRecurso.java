package com.trampix.recursos;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.trampix.entidades.Categoria;
import com.trampix.entidades.Subcategoria;
import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.Perfil;
import com.trampix.entidades.enums.StatusSubcategoria;
import com.trampix.repositorios.CategoriaRepositorio;
import com.trampix.repositorios.PrestadorPerfilRepositorio;
import com.trampix.repositorios.SubcategoriaRepositorio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// endpoints de subcategoria
@RestController
@RequestMapping("/categorias/{categoriaId}/subcategorias")
public class SubcategoriaRecurso {

    public record SubcategoriaRequisicao(
            @NotBlank @Size(min = 3, max = 40) String nome,
            @Size(max = 200) String descricao) {
    }

    public record AtualizarRequisicao(
            @NotBlank @Size(min = 3, max = 40) String nome,
            @Size(max = 200) String descricao,
            StatusSubcategoria status) {
    }

    private final SubcategoriaRepositorio repositorio;
    private final CategoriaRepositorio categoriaRepositorio;
    private final PrestadorPerfilRepositorio prestadorRepositorio;
    private final CriadorSubcategoria criador;

    public SubcategoriaRecurso(SubcategoriaRepositorio repositorio, CategoriaRepositorio categoriaRepositorio,
            PrestadorPerfilRepositorio prestadorRepositorio, CriadorSubcategoria criador) {
        this.repositorio = repositorio;
        this.categoriaRepositorio = categoriaRepositorio;
        this.prestadorRepositorio = prestadorRepositorio;
        this.criador = criador;
    }

    // lista as subcategorias ativas da categoria
    @GetMapping
    public List<SubcategoriaResposta> listar(@PathVariable Long categoriaId) {
        buscarCategoriaOu404(categoriaId);
        return repositorio.findByCategoriaIdAndStatusOrderById(categoriaId, StatusSubcategoria.ATIVA).stream()
                .map(SubcategoriaResposta::de).toList();
    }

    // cria a subcategoria ou devolve a que já existe
    @PostMapping
    @Transactional
    public ResponseEntity<SubcategoriaResposta> criar(@PathVariable Long categoriaId,
            @Valid @RequestBody SubcategoriaRequisicao requisicao, @AuthenticationPrincipal Usuario logado) {
        boolean admin = logado.getPerfil() == Perfil.ADMIN;
        if (!admin && !prestadorRepositorio.existsByUsuarioId(logado.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "só prestador ou ADMIN pode criar subcategoria");
        }
        Categoria categoria = buscarCategoriaOu404(categoriaId);
        CriadorSubcategoria.Resultado resultado = criador.obterOuCriar(categoria, requisicao.nome(),
                requisicao.descricao(), logado);
        Subcategoria subcategoria = resultado.subcategoria();
        if (subcategoria.getStatus() != StatusSubcategoria.ATIVA) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "já existe a subcategoria " + subcategoria.getNome() + " nesta categoria, mas está inativa");
        }
        return ResponseEntity.status(resultado.criada() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(SubcategoriaResposta.de(subcategoria));
    }

    // altera nome, descrição e status
    @PutMapping("/{id}")
    @Transactional
    public SubcategoriaResposta atualizar(@PathVariable Long categoriaId, @PathVariable Long id,
            @Valid @RequestBody AtualizarRequisicao requisicao) {
        Subcategoria subcategoria = buscarOu404(categoriaId, id);
        CriadorSubcategoria.exigirNomeValido(requisicao.nome());
        if (repositorio.existsByCategoriaIdAndNomeNormalizadoAndIdNot(categoriaId,
                Subcategoria.normalizar(requisicao.nome()), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "já existe uma subcategoria com esse nome nesta categoria");
        }
        subcategoria.setNome(requisicao.nome());
        subcategoria.setDescricao(requisicao.descricao());
        if (requisicao.status() != null) {
            subcategoria.setStatus(requisicao.status());
        }
        return SubcategoriaResposta.de(repositorio.save(subcategoria));
    }

    // marca a subcategoria como inativa
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void inativar(@PathVariable Long categoriaId, @PathVariable Long id) {
        Subcategoria subcategoria = buscarOu404(categoriaId, id);
        subcategoria.setStatus(StatusSubcategoria.INATIVA);
        repositorio.save(subcategoria);
    }

    private Categoria buscarCategoriaOu404(Long categoriaId) {
        return categoriaRepositorio.findById(categoriaId)
                .filter(Categoria::isAtiva)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "categoria não encontrada"));
    }

    private Subcategoria buscarOu404(Long categoriaId, Long id) {
        return repositorio.findByIdAndCategoriaId(id, categoriaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "subcategoria não encontrada"));
    }
}
