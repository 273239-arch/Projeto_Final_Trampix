package com.trampix.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trampix.entidades.Categoria;

public interface CategoriaRepositorio extends JpaRepository<Categoria, Long> {

    List<Categoria> findByAtivaTrueOrderById();

    Optional<Categoria> findFirstByCategoriaPaiIsNullAndNomeIgnoreCase(String nome);

    // verifica nome repetido no mesmo pai
    boolean existsByCategoriaPaiIdAndNomeIgnoreCase(Long categoriaPaiId, String nome);

    boolean existsByCategoriaPaiIsNullAndNomeIgnoreCase(String nome);

    boolean existsByCategoriaPaiIdAndNomeIgnoreCaseAndIdNot(Long categoriaPaiId, String nome, Long id);

    boolean existsByCategoriaPaiIsNullAndNomeIgnoreCaseAndIdNot(String nome, Long id);
}
