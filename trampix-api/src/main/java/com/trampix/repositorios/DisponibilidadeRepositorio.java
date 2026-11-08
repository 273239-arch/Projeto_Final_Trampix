package com.trampix.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.trampix.entidades.Disponibilidade;

public interface DisponibilidadeRepositorio extends JpaRepository<Disponibilidade, Long> {

    List<Disponibilidade> findByPrestadorId(Long prestadorId);

    // apaga os horários do prestador
    @Modifying
    @Query("delete from Disponibilidade d where d.prestador.id = :prestadorId")
    void apagarDoPrestador(Long prestadorId);
}
