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

// Login por e-mail e senha. Devolve o token JWT, que vai no header
// "Authorization: Bearer <token>" das próximas requisições.
// TODO(recuperação de senha): POST /auth/esqueci-senha e /auth/redefinir-senha
// usando token_recuperacao_hash e token_recuperacao_expira_em de Usuario.
@RestController
@RequestMapping("/auth")
public class AuthRecurso {

    public record LoginRequisicao(@NotBlank @Email String email, @NotBlank String senha) {
    }

    // "usuario" sai sem senha e sem hash (Usuario já esconde os dois no JSON).
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
        // Mesma mensagem para e-mail inexistente e senha errada: não revela quem tem conta.
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
