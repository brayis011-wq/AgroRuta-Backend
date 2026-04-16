package com.agroruta.worker.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface JpaCargoRepository extends JpaRepository<CargoEntity, Long> {
    List<CargoEntity> findByActivoTrue();
}