package com.agroruta.worker.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface JpaActividadRepository extends JpaRepository<ActividadEntity, Long> {
    List<ActividadEntity> findByActivaTrue();
}