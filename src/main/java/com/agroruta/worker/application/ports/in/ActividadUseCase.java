package com.agroruta.worker.application.ports.in;

import com.agroruta.worker.domain.Actividad;
import java.util.List;

public interface ActividadUseCase {

    /** Crea una labor común (Administrador). */
    Actividad crearActividad(String nombre, String descripcion);

    /** Crea una labor del agricultor; con agricultorId null es una labor común. */
    Actividad crearActividad(String nombre, String descripcion, Long agricultorId);

    Actividad actualizarActividad(Long id, String nombre, String descripcion);

    void desactivarActividad(Long id);

    Actividad buscarPorId(Long id);

    List<Actividad> listarActivas();

    /** Activas que puede usar el agricultor: las comunes y las suyas. */
    List<Actividad> listarActivasPara(Long agricultorId);

    List<Actividad> listarTodas();
}