package com.trampix.entidades;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.trampix.entidades.enums.StatusSubcategoria;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// subcategoria de uma categoria de serviço
@Entity
@Table(name = "subcategoria", uniqueConstraints = @UniqueConstraint(
        name = "uk_subcategoria_categoria_nome", columnNames = { "categoria_id", "nome_normalizado" }))
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Getter
@Setter
@NoArgsConstructor
public class Subcategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 40)
    @Column(nullable = false, length = 40)
    private String nome;

    // nome sem acento e em minúsculas, para comparar
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    @Column(name = "nome_normalizado", nullable = false, length = 40)
    private String nomeNormalizado;

    @Size(max = 200)
    @Column(length = 200)
    private String descricao;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private boolean padrao = false;

    // usuário que criou (nulo nas padrão)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criada_por_id")
    private Usuario criadaPor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private StatusSubcategoria status = StatusSubcategoria.ATIVA;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    // guarda o nome limpo e o nome normalizado
    public void setNome(String nome) {
        this.nome = limpar(nome);
        this.nomeNormalizado = normalizar(nome);
    }

    // tira espaços das pontas e espaços repetidos
    public static String limpar(String nome) {
        return nome == null ? null : nome.trim().replaceAll("\\s+", " ");
    }

    // tira acentos e deixa em minúsculas
    public static String normalizar(String nome) {
        String limpo = limpar(nome);
        if (limpo == null) {
            return null;
        }
        return Normalizer.normalize(limpo, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
