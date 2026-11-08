package com.trampix.recursos;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.trampix.entidades.Contratacao;
import com.trampix.entidades.ContratacaoHistorico;
import com.trampix.entidades.enums.StatusContratacao;
import com.trampix.repositorios.ContratacaoHistoricoRepositorio;
import com.trampix.repositorios.ContratacaoRepositorio;

// marca como EXPIRADA o pedido ou a contraproposta sem resposta no prazo
@Component
public class ExpiradorContratacao {

    private final ContratacaoRepositorio repositorio;
    private final ContratacaoHistoricoRepositorio historicoRepositorio;

    public ExpiradorContratacao(ContratacaoRepositorio repositorio,
            ContratacaoHistoricoRepositorio historicoRepositorio) {
        this.repositorio = repositorio;
        this.historicoRepositorio = historicoRepositorio;
    }

    // grava em transação própria
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expirarVencidas() {
        LocalDateTime agora = LocalDateTime.now();
        for (ContratacaoRepositorio.Vencida vencida : repositorio.vencidas(StatusContratacao.PENDENTE,
                StatusContratacao.CONTRAPROPOSTA, agora.minus(ContratacaoRecurso.PRAZO_RESPOSTA))) {
            if (repositorio.trocarStatus(vencida.getId(), vencida.getStatus(), StatusContratacao.EXPIRADA, agora) == 1) {
                Contratacao contratacao = repositorio.getReferenceById(vencida.getId());
                historicoRepositorio.save(new ContratacaoHistorico(contratacao, null, vencida.getStatus(),
                        "prazo de resposta vencido"));
            }
        }
    }
}
