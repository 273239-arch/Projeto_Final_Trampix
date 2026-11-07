package com.trampix.entidades;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Catálogo independente de categorias. Uma categoria com "pai" é uma subcategoria.
// O nome é único dentro do mesmo pai: "Pintura" pode existir em "Reformas" e em "Artes".
// Atenção: no MySQL, NULL não conta como repetido no UNIQUE, então a unicidade das
// categorias raiz (sem pai) é conferida no CategoriaRecurso.
@Entity
@Table(name = "categoria", uniqueConstraints = @UniqueConstraint(
        name = "uk_categoria_pai_nome", columnNames = { "categoria_pai_id", "nome" }))
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Getter
@Setter
@NoArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String nome;

    @Size(max = 255)
    @Column(length = 255)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_pai_id")
    private Categoria categoriaPai;

    // Exclusão lógica: serviços antigos continuam apontando para a categoria.
    @Column(nullable = false)
    private boolean ativa = true;
}
