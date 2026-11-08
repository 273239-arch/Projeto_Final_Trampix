package com.trampix.recursos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.trampix.entidades.Categoria;
import com.trampix.entidades.PrestadorPerfil;
import com.trampix.entidades.Servico;
import com.trampix.entidades.Subcategoria;
import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.StatusServico;
import com.trampix.entidades.enums.TipoCobranca;

// dados do serviço devolvidos no JSON
public record ServicoResposta(
        Long id,
        String titulo,
        String descricao,
        BigDecimal preco,
        TipoCobranca tipoCobranca,
        StatusServico status,
        Resumo categoria,
        Resumo subcategoria,
        PrestadorResumo prestador,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    public record Resumo(Long id, String nome) {
    }

    public record PrestadorResumo(Long id, Long usuarioId, String nome, String cidade, String uf, boolean verificado) {
    }

    public static ServicoResposta de(Servico servico) {
        Categoria categoria = servico.getCategoria();
        Subcategoria subcategoria = servico.getSubcategoria();
        PrestadorPerfil prestador = servico.getPrestador();
        Usuario usuario = prestador.getUsuario();
        return new ServicoResposta(
                servico.getId(),
                servico.getTitulo(),
                servico.getDescricao(),
                servico.getPreco(),
                servico.getTipoCobranca(),
                servico.getStatus(),
                new Resumo(categoria.getId(), categoria.getNome()),
                new Resumo(subcategoria.getId(), subcategoria.getNome()),
                new PrestadorResumo(prestador.getId(), usuario.getId(), usuario.getNome(),
                        usuario.getCidade(), usuario.getUf(), prestador.isVerificado()),
                servico.getCriadoEm(),
                servico.getAtualizadoEm());
    }
}
