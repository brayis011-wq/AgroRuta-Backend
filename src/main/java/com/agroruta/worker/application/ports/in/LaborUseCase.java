package com.agroruta.worker.application.ports.in;

import com.agroruta.worker.domain.Labor;
import java.util.List;

public interface LaborUseCase {

    /** Crea una labor común (Administrador). */
    Labor crearActividad(String nombre, String descripcion);

    /** Crea una labor del agricultor; con agricultorId null es una labor común. */
    Labor crearActividad(String nombre, String descripcion, Long agricultorId);

    Labor actualizarActividad(Long id, String nombre, String descripcion);

    void desactivarActividad(Long id);

    Labor buscarPorId(Long id);

    List<Labor> listarActivas();

    /** Activas que puede usar el agricultor: las comunes y las suyas. */
    List<Labor> listarActivasPara(Long agricultorId);

    List<Labor> listarTodas();
}