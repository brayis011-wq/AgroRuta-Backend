package com.bg.agroruta.cultivo.domain;

import java.util.List;
import java.util.Optional;

public interface ActividadCultivoRepository {
    ActividadCultivo save(ActividadCultivo actividad);
    Optional<ActividadCultivo> findById(Long id);
    List<ActividadCultivo> findBySiembraId(Long siembraId);
    void deleteById(Long id);
}