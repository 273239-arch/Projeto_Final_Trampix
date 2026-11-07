package com.trampix.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trampix.entidades.Servico;
import com.trampix.entidades.enums.StatusServico;

public interface ServicoRepositorio extends JpaRepository<Servico, Long> {

    List<Servico> findByPrestadorId(Long prestadorId);

    List<Servico> findByCategoriaIdAndStatus(Long categoriaId, StatusServico status);
}
