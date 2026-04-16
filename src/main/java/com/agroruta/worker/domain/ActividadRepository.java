package com.agroruta.worker.domain;

import java.util.List;
import java.util.Optional;

public interface ActividadRepository {
    Actividad guardar(Actividad actividad);
    Optional<Actividad> buscarPorId(Long id);
    List<Actividad> listarTodas();
    List<Actividad> listarActivas();
    void eliminar(Long id);
}