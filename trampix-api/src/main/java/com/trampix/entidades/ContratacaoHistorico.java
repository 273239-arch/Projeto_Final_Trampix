package com.trampix.entidades;

import java.time.LocalDateTime;

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

    // usuário que mudou o status
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false, updatable = false)
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

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public ContratacaoHistorico(Contratacao contratacao, Usuario autor, StatusContratacao statusAnterior,
            StatusContratacao statusNovo) {
        this.contratacao = contratacao;
        this.autor = autor;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
    }
}
