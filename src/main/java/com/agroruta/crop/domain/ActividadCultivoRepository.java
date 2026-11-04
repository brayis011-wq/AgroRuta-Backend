package com.agroruta.crop.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ActividadCultivoRepository {
    ActividadCultivo save(ActividadCultivo actividad);
    Optional<ActividadCultivo> findById(Long id);
    List<ActividadCultivo> findBySiembraId(Long siembraId);
    void deleteById(Long id);
    void deleteBySiembraId(Long siembraId);
    boolean existsByTipoAndFechaAndSiembraId(TipoActividad tipo, LocalDate fecha, Long siembraId);  // ← nuevo
}