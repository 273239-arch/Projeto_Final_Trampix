package com.trampix.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trampix.entidades.Categoria;

public interface CategoriaRepositorio extends JpaRepository<Categoria, Long> {

    List<Categoria> findByAtivaTrue();

    // Nome único dentro do mesmo pai. Para categorias raiz usa as versões "IsNull",
    // porque o UNIQUE do MySQL não barra dois NULL iguais.
    boolean existsByCategoriaPaiIdAndNomeIgnoreCase(Long categoriaPaiId, String nome);

    boolean existsByCategoriaPaiIsNullAndNomeIgnoreCase(String nome);

    boolean existsByCategoriaPaiIdAndNomeIgnoreCaseAndIdNot(Long categoriaPaiId, String nome, Long id);

    boolean existsByCategoriaPaiIsNullAndNomeIgnoreCaseAndIdNot(String nome, Long id);
}
