package com.trampix.entidades;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.trampix.entidades.enums.StatusContratacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// histórico de mudanças de status da contratação
@Entity
@Immutable
@Table(name = "contratacao_historico")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Getter
@NoArgsConstructor
public class ContratacaoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contratacao_id", nullable = false, updatable = false)
    private Contratacao contratacao;

    // usuário que mudou o status (nulo quando foi o sistema)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", updatable = false)
    private Usuario autor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status_anterior", length = 20, updatable = false)
    private StatusContratacao statusAnterior;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status_novo", nullable = false, length = 20, updatable = false)
    private StatusContratacao statusNovo;

    // cópia dos dados da contratação neste passo
    @Column(name = "data_inicio", nullable = false, updatable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false, updatable = false)
    private LocalDate dataFim;

    @Column(name = "hora_inicio", updatable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", updatable = false)
    private LocalTime horaFim;

    @Column(name = "preco_combinado", nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal precoCombinado;

    @Column(length = 500, updatable = false)
    private String motivo;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    // monta a linha com os dados atuais da contratação
    public ContratacaoHistorico(Contratacao contratacao, Usuario autor, StatusContratacao statusAnterior,
            String motivo) {
        this.contratacao = contratacao;
        this.autor = autor;
        this.statusAnterior = statusAnterior;
        this.statusNovo = contratacao.getStatus();
        this.dataInicio = contratacao.getDataInicio();
        this.dataFim = contratacao.getDataFim();
        this.horaInicio = contratacao.getHoraInicio();
        this.horaFim = contratacao.getHoraFim();
        this.precoCombinado = contratacao.getPrecoCombinado();
        this.motivo = motivo;
    }
}
