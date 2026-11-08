package com.trampix.entidades;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// dados de quem presta serviço
@Entity
@Table(name = "prestador_perfil")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Getter
@Setter
@NoArgsConstructor
public class PrestadorPerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Size(max = 1000)
    @Column(length = 1000)
    private String biografia;

    @Min(0)
    @Column(name = "raio_atendimento_km")
    private Integer raioAtendimentoKm;

    @Column(name = "agenda_cheia", nullable = false)
    private boolean agendaCheia = false;

    // selo de perfil verificado
    @Column(nullable = false)
    private boolean verificado = false;

    @Column(name = "verificado_em")
    private LocalDateTime verificadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verificado_por_id")
    private Usuario verificadoPor;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
