package com.trampix.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trampix.entidades.PrestadorPerfil;

public interface PrestadorPerfilRepositorio extends JpaRepository<PrestadorPerfil, Long> {

    Optional<PrestadorPerfil> findByUsuarioId(Long usuarioId);

    // Decisão: ser prestador = ter um prestador_perfil.
    boolean existsByUsuarioId(Long usuarioId);
}
