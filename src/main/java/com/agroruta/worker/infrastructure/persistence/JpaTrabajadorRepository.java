package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.domain.EstadoTrabajador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface JpaTrabajadorRepository extends JpaRepository<TrabajadorEntity, Long> {
    Optional<TrabajadorEntity> findByCedula(String cedula);
    boolean existsByCedula(String cedula);
    List<TrabajadorEntity> findByEstado(EstadoTrabajador estado);
}