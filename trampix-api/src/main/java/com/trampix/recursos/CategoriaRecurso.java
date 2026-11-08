package com.trampix.recursos;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
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
import com.trampix.entidades.enums.StatusSubcategoria;
import com.trampix.repositorios.CategoriaRepositorio;
import com.trampix.repositorios.SubcategoriaRepositorio;

import jakarta.validation.Valid;

// endpoints de categoria
@RestController
@RequestMapping("/categorias")
public class CategoriaRecurso {

    private final CategoriaRepositorio repositorio;
    private final SubcategoriaRepositorio subcategoriaRepositorio;

    public CategoriaRecurso(CategoriaRepositorio repositorio, SubcategoriaRepositorio subcategoriaRepositorio) {
        this.repositorio = repositorio;
        this.subcategoriaRepositorio = subcategoriaRepositorio;
    }

    // lista as categorias ativas com as subcategorias ativas
    @GetMapping
    @Transactional(readOnly = true)
    public List<CategoriaResposta> listar() {
        Map<Long, List<SubcategoriaResposta>> porCategoria = subcategoriaRepositorio
                .findByStatusOrderById(StatusSubcategoria.ATIVA).stream()
                .map(SubcategoriaResposta::de)
                .collect(Collectors.groupingBy(SubcategoriaResposta::categoriaId));
        return repositorio.findByAtivaTrueOrderById().stream()
                .map(c -> CategoriaResposta.de(c, porCategoria.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CategoriaResposta buscar(@PathVariable Long id) {
        return resposta(buscarOu404(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResposta cadastrar(@Valid @RequestBody Categoria categoria) {
        if (repositorio.existsByNomeIgnoreCase(categoria.getNome())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "já existe uma categoria com esse nome");
        }
        categoria.setId(null);
        categoria.setAtiva(true);
        return resposta(repositorio.save(categoria));
    }

    @PutMapping("/{id}")
    public CategoriaResposta atualizar(@PathVariable Long id, @Valid @RequestBody Categoria dados) {
        Categoria categoria = buscarOu404(id);
        if (repositorio.existsByNomeIgnoreCaseAndIdNot(dados.getNome(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "já existe uma categoria com esse nome");
        }
        categoria.setNome(dados.getNome());
        categoria.setDescricao(dados.getDescricao());
        return resposta(repositorio.save(categoria));
    }

    // marca a categoria como inativa
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        Categoria categoria = buscarOu404(id);
        categoria.setAtiva(false);
        repositorio.save(categoria);
    }

    // monta a resposta com as subcategorias ativas
    private CategoriaResposta resposta(Categoria categoria) {
        return CategoriaResposta.de(categoria, subcategoriaRepositorio
                .findByCategoriaIdAndStatusOrderById(categoria.getId(), StatusSubcategoria.ATIVA).stream()
                .map(SubcategoriaResposta::de).toList());
    }

    private Categoria buscarOu404(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "categoria não encontrada"));
    }
}
