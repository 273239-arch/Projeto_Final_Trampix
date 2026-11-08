package com.trampix.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trampix.entidades.Subcategoria;
import com.trampix.entidades.enums.StatusSubcategoria;

public interface SubcategoriaRepositorio extends JpaRepository<Subcategoria, Long> {

    Optional<Subcategoria> findByCategoriaIdAndNomeNormalizado(Long categoriaId, String nomeNormalizado);

    boolean existsByCategoriaIdAndNomeNormalizadoAndIdNot(Long categoriaId, String nomeNormalizado, Long id);

    List<Subcategoria> findByStatusOrderById(StatusSubcategoria status);

    List<Subcategoria> findByCategoriaIdAndStatusOrderById(Long categoriaId, StatusSubcategoria status);

    Optional<Subcategoria> findByIdAndCategoriaId(Long id, Long categoriaId);
}
