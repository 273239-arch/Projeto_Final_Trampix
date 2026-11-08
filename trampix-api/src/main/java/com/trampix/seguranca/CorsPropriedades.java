package com.trampix.seguranca;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

// configurações do CORS
@ConfigurationProperties(prefix = "trampix.cors")
public record CorsPropriedades(List<String> origens) {
}
