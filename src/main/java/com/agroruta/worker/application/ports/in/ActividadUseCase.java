package com.agroruta.worker.application.ports.in;

import com.agroruta.worker.domain.Actividad;
import java.util.List;

public interface ActividadUseCase {

    Actividad crearActividad(String nombre, String descripcion);

    Actividad actualizarActividad(Long id, String nombre, String descripcion);

    void desactivarActividad(Long id);

    Actividad buscarPorId(Long id);

    List<Actividad> listarActivas();

    List<Actividad> listarTodas();
}