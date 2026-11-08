package com.trampix.recursos;

import java.util.List;

import com.trampix.entidades.Categoria;

// categoria com as subcategorias ativas
public record CategoriaResposta(Long id, String nome, String descricao, List<SubcategoriaResposta> subcategorias) {

    public static CategoriaResposta de(Categoria c, List<SubcategoriaResposta> subcategorias) {
        return new CategoriaResposta(c.getId(), c.getNome(), c.getDescricao(), subcategorias);
    }
}
