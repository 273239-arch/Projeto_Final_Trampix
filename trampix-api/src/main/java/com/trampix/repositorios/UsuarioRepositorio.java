package com.trampix.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trampix.entidades.Usuario;
import com.trampix.entidades.enums.Perfil;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByDocumento(String documento);

    boolean existsByPerfil(Perfil perfil);
}
