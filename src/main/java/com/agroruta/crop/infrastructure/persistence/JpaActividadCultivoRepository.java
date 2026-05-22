package com.agroruta.crop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.agroruta.crop.domain.TipoActividad;
import java.time.LocalDate;
public interface JpaActividadCultivoRepository extends JpaRepository<ActividadCultivoEntity, Long> {
    List<ActividadCultivoEntity> findBySiembraId(Long siembraId);
    boolean existsByTipoAndFechaAndSiembraId(TipoActividad tipo, LocalDate fecha, Long siembraId);  // ← nuevo
}