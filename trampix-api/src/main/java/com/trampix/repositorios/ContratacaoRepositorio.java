package com.trampix.repositorios;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.trampix.entidades.Contratacao;
import com.trampix.entidades.enums.StatusContratacao;

public interface ContratacaoRepositorio extends JpaRepository<Contratacao, Long> {

    // id e status de uma contratação vencida
    interface Vencida {
        Long getId();

        StatusContratacao getStatus();
    }

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

    // contratações do prestador com o status informado que cruzam as datas
    @Query("""
            select c from Contratacao c
            where c.servico.prestador.id = :prestadorId and c.status = :status
              and c.dataInicio <= :fim and c.dataFim >= :inicio
              and c.id <> :idIgnorado""")
    List<Contratacao> doPrestadorNasDatas(Long prestadorId, StatusContratacao status, LocalDate inicio,
            LocalDate fim, Long idIgnorado);

    // pedidos e contrapropostas sem resposta até o limite
    @Query("""
            select c.id as id, c.status as status from Contratacao c
            where (c.status = :pendente and c.criadoEm < :limite)
               or (c.status = :contraproposta and c.contrapropostaEm < :limite)
            order by c.id""")
    List<Vencida> vencidas(StatusContratacao pendente, StatusContratacao contraproposta, LocalDateTime limite);

    // muda o status só se ele ainda for o anterior
    @Modifying
    @Query("""
            update Contratacao c set c.status = :novo, c.atualizadoEm = :agora
            where c.id = :id and c.status = :anterior""")
    int trocarStatus(Long id, StatusContratacao anterior, StatusContratacao novo, LocalDateTime agora);
}
