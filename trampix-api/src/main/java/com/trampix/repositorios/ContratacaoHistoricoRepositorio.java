package com.trampix.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import com.trampix.entidades.ContratacaoHistorico;

// repositório do histórico (só salva e lista)
public interface ContratacaoHistoricoRepositorio extends Repository<ContratacaoHistorico, Long> {

    ContratacaoHistorico save(ContratacaoHistorico historico);

    @EntityGraph(attributePaths = { "autor" })
    List<ContratacaoHistorico> findByContratacaoIdOrderByCriadoEmAscIdAsc(Long contratacaoId);
}
