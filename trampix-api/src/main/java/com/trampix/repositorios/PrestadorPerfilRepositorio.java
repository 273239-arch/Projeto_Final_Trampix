package com.trampix.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.trampix.entidades.PrestadorPerfil;

import jakarta.persistence.LockModeType;

public interface PrestadorPerfilRepositorio extends JpaRepository<PrestadorPerfil, Long> {

    Optional<PrestadorPerfil> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioId(Long usuarioId);

    // busca o prestador com trava
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PrestadorPerfil p where p.id = :id")
    Optional<PrestadorPerfil> travar(Long id);
}
