package com.trampix.recursos;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.trampix.entidades.Categoria;
import com.trampix.entidades.Subcategoria;
import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.Perfil;
import com.trampix.entidades.enums.StatusSubcategoria;
import com.trampix.repositorios.CategoriaRepositorio;
import com.trampix.repositorios.SubcategoriaRepositorio;

// cria ou reaproveita subcategoria
@Component
public class CriadorSubcategoria {

    public record Resultado(Subcategoria subcategoria, boolean criada) {
    }

    private final SubcategoriaRepositorio repositorio;
    private final CategoriaRepositorio categoriaRepositorio;

    public CriadorSubcategoria(SubcategoriaRepositorio repositorio, CategoriaRepositorio categoriaRepositorio) {
        this.repositorio = repositorio;
        this.categoriaRepositorio = categoriaRepositorio;
    }

    // devolve a existente com o mesmo nome ou cria uma nova
    public Resultado obterOuCriar(Categoria categoria, String nome, String descricao, Usuario autor) {
        exigirNomeValido(nome);
        var existente = repositorio.findByCategoriaIdAndNomeNormalizado(categoria.getId(),
                Subcategoria.normalizar(nome));
        if (existente.isPresent()) {
            return new Resultado(existente.get(), false);
        }
        boolean admin = autor.getPerfil() == Perfil.ADMIN;
        Subcategoria nova = new Subcategoria();
        nova.setNome(nome);
        nova.setDescricao(descricao);
        nova.setCategoria(categoria);
        nova.setPadrao(admin);
        nova.setCriadaPor(admin ? null : autor);
        nova.setStatus(StatusSubcategoria.ATIVA);
        return new Resultado(repositorio.save(nova), true);
    }

    // escolhe a subcategoria do serviço pelo id ou pelo nome novo
    public Subcategoria paraServico(Long subcategoriaId, Long categoriaId, String novaSubcategoria, Usuario autor) {
        Subcategoria subcategoria;
        if (subcategoriaId != null) {
            if (novaSubcategoria != null) {
                throw invalido("informe subcategoriaId ou novaSubcategoria, não os dois");
            }
            subcategoria = repositorio.findById(subcategoriaId)
                    .orElseThrow(() -> invalido("subcategoria não encontrada"));
            if (categoriaId != null && !categoriaId.equals(subcategoria.getCategoria().getId())) {
                throw invalido("a subcategoria não pertence à categoria informada");
            }
        } else {
            if (categoriaId == null || novaSubcategoria == null) {
                throw invalido("informe subcategoriaId ou categoriaId + novaSubcategoria");
            }
            Categoria categoria = categoriaRepositorio.findById(categoriaId)
                    .filter(Categoria::isAtiva)
                    .orElseThrow(() -> invalido("categoria não encontrada"));
            subcategoria = obterOuCriar(categoria, novaSubcategoria, null, autor).subcategoria();
        }
        if (subcategoria.getStatus() != StatusSubcategoria.ATIVA || !subcategoria.getCategoria().isAtiva()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "a subcategoria " + subcategoria.getNome() + " está inativa");
        }
        return subcategoria;
    }

    // confere o tamanho do nome sem os espaços extras
    public static void exigirNomeValido(String nome) {
        String limpo = Subcategoria.limpar(nome);
        if (limpo == null || limpo.length() < 3 || limpo.length() > 40) {
            throw invalido("o nome da subcategoria deve ter de 3 a 40 caracteres");
        }
    }

    private static ResponseStatusException invalido(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
