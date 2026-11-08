package com.trampix.repositorios;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trampix.entidades.Categoria;

public interface CategoriaRepositorio extends JpaRepository<Categoria, Long> {

    List<Categoria> findByAtivaTrueOrderById();

    Optional<Categoria> findFirstByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
