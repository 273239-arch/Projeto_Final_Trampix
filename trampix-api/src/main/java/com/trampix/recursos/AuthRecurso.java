package com.trampix.recursos;

import java.time.Instant;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.StatusUsuario;
import com.trampix.repositorios.UsuarioRepositorio;
import com.trampix.seguranca.TokenServico;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// login com e-mail e senha
// TODO: recuperação de senha
@RestController
@RequestMapping("/auth")
public class AuthRecurso {

    public record LoginRequisicao(@NotBlank @Email String email, @NotBlank String senha) {
    }

    public record LoginResposta(String token, String tipo, Instant expiraEm, Usuario usuario) {
    }

    private final UsuarioRepositorio repositorio;
    private final PasswordEncoder passwordEncoder;
    private final TokenServico tokenServico;

    public AuthRecurso(UsuarioRepositorio repositorio, PasswordEncoder passwordEncoder, TokenServico tokenServico) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
        this.tokenServico = tokenServico;
    }

    @PostMapping("/login")
    public LoginResposta login(@Valid @RequestBody LoginRequisicao requisicao) {
        String email = requisicao.email().trim().toLowerCase(Locale.ROOT);
        // busca o usuário e confere a senha
        Usuario usuario = repositorio.findByEmail(email)
                .filter(u -> passwordEncoder.matches(requisicao.senha(), u.getSenhaHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "e-mail ou senha inválidos"));
        if (usuario.getStatus() != StatusUsuario.ATIVO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "login não permitido: conta com status " + usuario.getStatus().name());
        }
        TokenServico.Token token = tokenServico.gerar(usuario);
        return new LoginResposta(token.valor(), "Bearer", token.expiraEm(), usuario);
    }
}
