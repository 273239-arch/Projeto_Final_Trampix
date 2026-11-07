package com.trampix.seguranca;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.StatusUsuario;
import com.trampix.repositorios.UsuarioRepositorio;

// Depois que o Spring confere a assinatura e a validade do token, esta classe busca o usuário
// do "sub" no banco. Assim, nos recursos, @AuthenticationPrincipal Usuario logado é o usuário.
// Se a conta não estiver mais ATIVA (excluída, suspensa, banida), o token deixa de valer (401).
@Component
public class ConversorJwtUsuario implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepositorio repositorio;

    public ConversorJwtUsuario(UsuarioRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long id;
        try {
            id = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidBearerTokenException("token inválido");
        }
        Usuario usuario = repositorio.findById(id)
                .filter(u -> u.getStatus() == StatusUsuario.ATIVO)
                .orElseThrow(() -> new InvalidBearerTokenException("usuário não encontrado ou inativo"));
        // O perfil vem do banco, não do token: se um ADMIN for rebaixado, vale na hora.
        var permissoes = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()));
        return UsernamePasswordAuthenticationToken.authenticated(usuario, jwt, permissoes);
    }
}
