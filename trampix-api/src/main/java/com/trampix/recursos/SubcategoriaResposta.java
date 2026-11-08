package com.trampix.recursos;

import com.trampix.entidades.Subcategoria;
import com.trampix.entidades.enums.StatusSubcategoria;

// dados da subcategoria devolvidos no JSON
public record SubcategoriaResposta(Long id, String nome, String descricao, Long categoriaId, boolean padrao,
        StatusSubcategoria status) {

    public static SubcategoriaResposta de(Subcategoria s) {
        return new SubcategoriaResposta(s.getId(), s.getNome(), s.getDescricao(), s.getCategoria().getId(),
                s.isPadrao(), s.getStatus());
    }
}
