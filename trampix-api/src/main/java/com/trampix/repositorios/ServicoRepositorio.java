package com.trampix.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.trampix.entidades.Servico;
import com.trampix.entidades.enums.StatusServico;

public interface ServicoRepositorio extends JpaRepository<Servico, Long> {

    // lista os serviços com filtros opcionais de categoria e subcategoria
    @EntityGraph(attributePaths = { "prestador", "prestador.usuario", "categoria", "subcategoria" })
    @Query("""
            select s from Servico s
            where s.status = :status
              and (:categoriaId is null or s.categoria.id = :categoriaId)
              and (:subcategoriaId is null or s.subcategoria.id = :subcategoriaId)
            order by s.id""")
    List<Servico> filtrar(StatusServico status, Long categoriaId, Long subcategoriaId);

    List<Servico> findByPrestadorId(Long prestadorId);
}
