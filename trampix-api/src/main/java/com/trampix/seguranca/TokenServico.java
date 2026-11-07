package com.trampix.seguranca;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.trampix.entidades.Usuario;

// Gera o token JWT do login. A conferência do token fica com o Spring Security (SegurancaConfig).
@Component
public class TokenServico {

    public static final String EMISSOR = "trampix-api";

    public record Token(String valor, Instant expiraEm) {
    }

    private final JwtEncoder encoder;
    private final JwtPropriedades propriedades;

    public TokenServico(JwtEncoder encoder, JwtPropriedades propriedades) {
        this.encoder = encoder;
        this.propriedades = propriedades;
    }

    public Token gerar(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(propriedades.expiracao());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(usuario.getId().toString()) // "sub" = id do usuário
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim("perfil", usuario.getPerfil().name())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new Token(valor, expiraEm);
    }
}
