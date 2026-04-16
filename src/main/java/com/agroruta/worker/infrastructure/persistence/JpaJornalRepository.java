package com.agroruta.worker.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

interface JpaJornalRepository extends JpaRepository<JornalEntity, Long> {

    List<JornalEntity> findByTrabajadorId(Long trabajadorId);

    List<JornalEntity> findByTrabajadorIdAndFechaBetween(
            Long trabajadorId, LocalDate inicio, LocalDate fin);

    List<JornalEntity> findByCultivoId(Long cultivoId);

    List<JornalEntity> findByCultivoIdAndFechaBetween(
            Long cultivoId, LocalDate inicio, LocalDate fin);

    List<JornalEntity> findByTrabajadorIdAndLiquidadoFalse(Long trabajadorId);

    List<JornalEntity> findByTrabajadorIdAndLiquidadoFalseAndFechaBetween(
            Long trabajadorId, LocalDate inicio, LocalDate fin);
}