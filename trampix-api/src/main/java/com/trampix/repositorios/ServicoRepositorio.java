package com.trampix.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.trampix.entidades.Servico;
import com.trampix.entidades.enums.StatusServico;

public interface ServicoRepositorio extends JpaRepository<Servico, Long> {

    // @EntityGraph carrega prestador, usuário e categoria na mesma consulta (evita N+1).
    @EntityGraph(attributePaths = { "prestador", "prestador.usuario", "categoria" })
    List<Servico> findByStatus(StatusServico status);

    @EntityGraph(attributePaths = { "prestador", "prestador.usuario", "categoria" })
    List<Servico> findByCategoriaIdAndStatus(Long categoriaId, StatusServico status);

    List<Servico> findByPrestadorId(Long prestadorId);
}
