package com.trampix.recursos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.trampix.entidades.Categoria;
import com.trampix.entidades.PrestadorPerfil;
import com.trampix.entidades.Servico;
import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.StatusServico;
import com.trampix.entidades.enums.TipoCobranca;

// JSON público de um serviço. Mostra só um resumo do prestador: documento, e-mail,
// telefone e endereço completo do usuário NÃO saem aqui (LGPD).
public record ServicoResposta(
        Long id,
        String titulo,
        String descricao,
        BigDecimal preco,
        TipoCobranca tipoCobranca,
        StatusServico status,
        CategoriaResumo categoria,
        PrestadorResumo prestador,
        LocalDateTime criadoEm) {

    public record CategoriaResumo(Long id, String nome, Long categoriaPaiId) {
    }

    public record PrestadorResumo(Long id, Long usuarioId, String nome, String cidade, String uf, boolean verificado) {
    }

    public static ServicoResposta de(Servico servico) {
        Categoria categoria = servico.getCategoria();
        PrestadorPerfil prestador = servico.getPrestador();
        Usuario usuario = prestador.getUsuario();
        return new ServicoResposta(
                servico.getId(),
                servico.getTitulo(),
                servico.getDescricao(),
                servico.getPreco(),
                servico.getTipoCobranca(),
                servico.getStatus(),
                new CategoriaResumo(categoria.getId(), categoria.getNome(),
                        categoria.getCategoriaPai() == null ? null : categoria.getCategoriaPai().getId()),
                new PrestadorResumo(prestador.getId(), usuario.getId(), usuario.getNome(),
                        usuario.getCidade(), usuario.getUf(), prestador.isVerificado()),
                servico.getCriadoEm());
    }
}
