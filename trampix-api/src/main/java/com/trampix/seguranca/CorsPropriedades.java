package com.trampix.seguranca;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Lê trampix.cors.origens do application.properties (lista separada por vírgula).
@ConfigurationProperties(prefix = "trampix.cors")
public record CorsPropriedades(List<String> origens) {
}
