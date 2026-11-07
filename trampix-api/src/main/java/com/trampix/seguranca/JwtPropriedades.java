package com.trampix.seguranca;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Lê trampix.jwt.segredo e trampix.jwt.expiracao do application.properties.
@ConfigurationProperties(prefix = "trampix.jwt")
public record JwtPropriedades(String segredo, Duration expiracao) {

    public JwtPropriedades {
        // HS256 exige chave de pelo menos 256 bits (32 bytes). Falha logo na subida se for curta.
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("trampix.jwt.segredo precisa ter pelo menos 32 caracteres");
        }
        if (expiracao == null || expiracao.isNegative() || expiracao.isZero()) {
            throw new IllegalStateException("trampix.jwt.expiracao precisa ser maior que zero");
        }
    }
}
