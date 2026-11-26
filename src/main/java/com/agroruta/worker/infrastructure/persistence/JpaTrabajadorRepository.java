package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.domain.EstadoTrabajador;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaTrabajadorRepository extends JpaRepository<TrabajadorEntity, Long> {
    Optional<TrabajadorEntity> findFirstByUsuarioId(Long usuarioId);

    @Query(
            "SELECT t FROM TrabajadorEntity t WHERE UPPER(REPLACE(REPLACE(REPLACE(t.cedula, '.', ''), ' ', ''), '-', '')) = :cedula")
    List<TrabajadorEntity> findByCedulaNormalizada(@Param("cedula") String cedula);

    boolean existsByCedulaAndAgricultorId(String cedula, Long agricultorId);

    List<TrabajadorEntity> findByEstado(EstadoTrabajador estado);

    List<TrabajadorEntity> findByAgricultorId(Long agricultorId);

    List<TrabajadorEntity> findByAgricultorIdAndEstado(Long agricultorId, EstadoTrabajador estado);
}
