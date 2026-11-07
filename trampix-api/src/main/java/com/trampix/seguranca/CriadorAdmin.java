package com.trampix.seguranca;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.Perfil;
import com.trampix.entidades.enums.StatusUsuario;
import com.trampix.repositorios.UsuarioRepositorio;

// Ao subir a aplicação: se não existir nenhum ADMIN, cria um com o e-mail e a senha
// das variáveis TRAMPIX_ADMIN_EMAIL e TRAMPIX_ADMIN_SENHA (via application.properties).
// O ADMIN de sistema não tem documento (CPF/CNPJ). A senha nunca aparece no log.
@Component
public class CriadorAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CriadorAdmin.class);

    private final UsuarioRepositorio repositorio;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;

    public CriadorAdmin(UsuarioRepositorio repositorio, PasswordEncoder passwordEncoder,
            @Value("${trampix.admin.email:}") String email, @Value("${trampix.admin.senha:}") String senha) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
        this.email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        this.senha = senha == null ? "" : senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repositorio.existsByPerfil(Perfil.ADMIN)) {
            log.info("ADMIN já existe: nenhum ADMIN novo foi criado.");
            return;
        }
        if (email.isBlank() || senha.isBlank()) {
            log.warn("Não existe nenhum ADMIN e as variáveis TRAMPIX_ADMIN_EMAIL e TRAMPIX_ADMIN_SENHA "
                    + "não estão definidas: ADMIN não criado. Rotas só de ADMIN (ex.: POST /categorias) "
                    + "ficarão inacessíveis até que um ADMIN exista.");
            return;
        }
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            log.error("TRAMPIX_ADMIN_EMAIL não é um e-mail válido: ADMIN não criado.");
            return;
        }
        if (senha.length() < 8 || senha.length() > 72) {
            log.error("TRAMPIX_ADMIN_SENHA deve ter entre 8 e 72 caracteres: ADMIN não criado.");
            return;
        }
        // Não promove uma conta comum que já use esse e-mail: quem a criou conhece a senha dela.
        if (repositorio.existsByEmail(email)) {
            log.error("Já existe uma conta comum com o e-mail {}: ADMIN não criado. Use outro e-mail.", email);
            return;
        }
        Usuario admin = new Usuario();
        admin.setNome("Administrador");
        admin.setEmail(email);
        admin.setSenhaHash(passwordEncoder.encode(senha));
        admin.setPerfil(Perfil.ADMIN);
        admin.setStatus(StatusUsuario.ATIVO);
        repositorio.save(admin);
        log.info("ADMIN criado com o e-mail {}.", email);
    }
}
