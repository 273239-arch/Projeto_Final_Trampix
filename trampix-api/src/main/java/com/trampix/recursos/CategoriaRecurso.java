package com.trampix.recursos;

import java.util.List;

import org.springframework.http.HttpStatus;
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
import com.trampix.repositorios.CategoriaRepositorio;

import jakarta.validation.Valid;

// GET é público. Criar, alterar e excluir: só ADMIN (regra na SegurancaConfig).
@RestController
@RequestMapping("/categorias")
public class CategoriaRecurso {

    private final CategoriaRepositorio repositorio;

    public CategoriaRecurso(CategoriaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    // Lista só as ativas: é o catálogo que o front exibe.
    @GetMapping
    public List<Categoria> listar() {
        return repositorio.findByAtivaTrue();
    }

    @GetMapping("/{id}")
    public Categoria buscar(@PathVariable Long id) {
        return buscarOu404(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Categoria cadastrar(@Valid @RequestBody Categoria categoria) {
        Categoria pai = resolverPai(categoria.getCategoriaPai(), null);
        if (nomeRepetido(categoria.getNome(), pai, null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "já existe uma categoria com esse nome neste nível");
        }
        categoria.setId(null);
        categoria.setAtiva(true);
        categoria.setCategoriaPai(pai);
        return repositorio.save(categoria);
    }

    @PutMapping("/{id}")
    public Categoria atualizar(@PathVariable Long id, @Valid @RequestBody Categoria dados) {
        Categoria categoria = buscarOu404(id);
        Categoria pai = resolverPai(dados.getCategoriaPai(), id);
        if (nomeRepetido(dados.getNome(), pai, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "já existe uma categoria com esse nome neste nível");
        }
        categoria.setNome(dados.getNome());
        categoria.setDescricao(dados.getDescricao());
        categoria.setCategoriaPai(pai);
        return repositorio.save(categoria);
    }

    // Exclusão lógica: serviços antigos continuam apontando para a categoria.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        Categoria categoria = buscarOu404(id);
        categoria.setAtiva(false);
        repositorio.save(categoria);
    }

    // O JSON manda só {"categoriaPai": {"id": 1}}; aqui buscamos a categoria real.
    private Categoria resolverPai(Categoria pai, Long idAtual) {
        if (pai == null || pai.getId() == null) {
            return null;
        }
        if (pai.getId().equals(idAtual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "uma categoria não pode ser pai dela mesma");
        }
        return repositorio.findById(pai.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoria pai não encontrada"));
    }

    // Nome único dentro do mesmo pai (idAtual = null no cadastro). Raiz é tratada à parte
    // porque o UNIQUE (categoria_pai_id, nome) do MySQL deixa passar dois NULL iguais.
    private boolean nomeRepetido(String nome, Categoria pai, Long idAtual) {
        if (pai == null) {
            return idAtual == null
                    ? repositorio.existsByCategoriaPaiIsNullAndNomeIgnoreCase(nome)
                    : repositorio.existsByCategoriaPaiIsNullAndNomeIgnoreCaseAndIdNot(nome, idAtual);
        }
        return idAtual == null
                ? repositorio.existsByCategoriaPaiIdAndNomeIgnoreCase(pai.getId(), nome)
                : repositorio.existsByCategoriaPaiIdAndNomeIgnoreCaseAndIdNot(pai.getId(), nome, idAtual);
    }

    private Categoria buscarOu404(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "categoria não encontrada"));
    }
}
