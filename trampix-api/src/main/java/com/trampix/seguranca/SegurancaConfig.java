package com.trampix.seguranca;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableConfigurationProperties({ JwtPropriedades.class, CorsPropriedades.class })
public class SegurancaConfig {

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, ConversorJwtUsuario conversor,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
        // 401 e 403 no formato do TratadorErros
        AuthenticationEntryPoint naoAutenticado = (req, res, ex) -> resolver.resolveException(req, res, null, ex);
        AccessDeniedHandler semPermissao = (req, res, ex) -> resolver.resolveException(req, res, null, ex);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST, "/usuarios", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categorias", "/categorias/**").permitAll()
                        // serviços do prestador logado
                        .requestMatchers(HttpMethod.GET, "/servicos/meus").authenticated()
                        .requestMatchers(HttpMethod.GET, "/servicos", "/servicos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/usuarios/*/disponibilidades").permitAll()
                        // prestador ou ADMIN cria subcategoria
                        .requestMatchers(HttpMethod.POST, "/categorias/*/subcategorias").authenticated()
                        // rotas só de ADMIN
                        .requestMatchers(HttpMethod.GET, "/usuarios").hasRole("ADMIN")
                        .requestMatchers("/categorias", "/categorias/**").hasRole("ADMIN")
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversor))
                        .authenticationEntryPoint(naoAutenticado))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(semPermissao));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder(JwtPropriedades propriedades) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave(propriedades)));
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtPropriedades propriedades) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave(propriedades))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // confere a validade e o emissor do token
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TokenServico.EMISSOR));
        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsPropriedades propriedades) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(propriedades.origens());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", cors);
        return fonte;
    }

    private static SecretKey chave(JwtPropriedades propriedades) {
        return new SecretKeySpec(propriedades.segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
