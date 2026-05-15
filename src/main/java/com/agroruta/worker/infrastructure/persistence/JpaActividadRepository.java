package com.agroruta.worker.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface JpaActividadRepository extends JpaRepository<ActividadEntity, Long> {
    List<ActividadEntity> findByActivaTrue();
    boolean existsByNombre(String nombre);                        // ← nuevo
    boolean existsByNombreAndIdNot(String nombre, Long id);      // ← para validar en actualización
}