package com.trampix.repositorios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.trampix.entidades.Contratacao;
import com.trampix.entidades.enums.StatusContratacao;

public interface ContratacaoRepositorio extends JpaRepository<Contratacao, Long> {

    // lista as contratações com filtros opcionais
    @EntityGraph(attributePaths = { "servico", "servico.prestador", "servico.prestador.usuario", "cliente" })
    @Query("""
            select c from Contratacao c
            where (:servicoId is null or c.servico.id = :servicoId)
              and (:usuarioId is null
                   or (:comoCliente = true and c.cliente.id = :usuarioId)
                   or (:comoPrestador = true and c.servico.prestador.usuario.id = :usuarioId))
            order by c.criadoEm desc, c.id desc""")
    List<Contratacao> filtrar(Long usuarioId, boolean comoCliente, boolean comoPrestador, Long servicoId);

    // verifica se tem contratação no intervalo
    @Query("""
            select count(c) > 0 from Contratacao c
            where c.servico.prestador.id = :prestadorId and c.status = :status
              and c.dataHoraDesejada > :inicio and c.dataHoraDesejada < :fim
              and c.id <> :idIgnorado""")
    boolean existeNoIntervalo(Long prestadorId, StatusContratacao status, LocalDateTime inicio,
            LocalDateTime fim, Long idIgnorado);
}
