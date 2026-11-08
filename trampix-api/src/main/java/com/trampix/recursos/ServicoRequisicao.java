package com.trampix.recursos;

import java.math.BigDecimal;

import com.trampix.entidades.Servico;
import com.trampix.entidades.Subcategoria;
import com.trampix.entidades.enums.TipoCobranca;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// corpo para cadastrar ou editar serviço
public record ServicoRequisicao(
        Long subcategoriaId,
        Long categoriaId,
        @Size(max = 40) String novaSubcategoria,
        @NotBlank @Size(max = 120) String titulo,
        @NotBlank @Size(max = 2000) String descricao,
        @NotNull @DecimalMin(value = "0.01", message = "o preço deve ser maior que zero")
        @Digits(integer = 8, fraction = 2) BigDecimal preco,
        @NotNull TipoCobranca tipoCobranca) {

    // copia os dados para o serviço
    public void aplicar(Servico servico, Subcategoria subcategoria) {
        servico.setCategoria(subcategoria.getCategoria());
        servico.setSubcategoria(subcategoria);
        servico.setTitulo(titulo);
        servico.setDescricao(descricao);
        servico.setPreco(preco);
        servico.setTipoCobranca(tipoCobranca);
    }
}
