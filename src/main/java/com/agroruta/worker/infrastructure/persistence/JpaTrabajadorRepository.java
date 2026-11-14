package com.agroruta.worker.infrastructure.persistence;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import com.agroruta.worker.domain.EstadoTrabajador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface JpaTrabajadorRepository extends JpaRepository<TrabajadorEntity, Long> {
    Optional<TrabajadorEntity> findFirstByUsuarioId(Long usuarioId);

    @Query("SELECT t FROM TrabajadorEntity t WHERE UPPER(REPLACE(REPLACE(REPLACE(t.cedula, '.', ''), ' ', ''), '-', '')) = :cedula")
    List<TrabajadorEntity> findByCedulaNormalizada(@Param("cedula") String cedula);
    boolean existsByCedulaAndAgricultorId(String cedula, Long agricultorId);
    List<TrabajadorEntity> findByEstado(EstadoTrabajador estado);
    List<TrabajadorEntity> findByAgricultorId(Long agricultorId);
    List<TrabajadorEntity> findByAgricultorIdAndEstado(Long agricultorId, EstadoTrabajador estado);
}