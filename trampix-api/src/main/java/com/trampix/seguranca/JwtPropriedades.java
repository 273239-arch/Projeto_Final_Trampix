package com.trampix.seguranca;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// configurações do JWT
@ConfigurationProperties(prefix = "trampix.jwt")
public record JwtPropriedades(String segredo, Duration expiracao) {

    public JwtPropriedades {
        // confere o segredo e a validade
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("trampix.jwt.segredo precisa ter pelo menos 32 caracteres");
        }
        if (expiracao == null || expiracao.isNegative() || expiracao.isZero()) {
            throw new IllegalStateException("trampix.jwt.expiracao precisa ser maior que zero");
        }
    }
}
