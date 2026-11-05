package com.agroruta.crop.application.ports.in;

import com.agroruta.crop.domain.ActividadCultivo;
import java.time.LocalDate;
import java.util.List;

public interface ActividadCultivoUseCase {
    /** costo: materiales o servicios de la actividad (opcional) */
    ActividadCultivo registrarActividad(String tipo, String descripcion,
                                        LocalDate fecha, Long siembraId, Double costo);

    default ActividadCultivo registrarActividad(String tipo, String descripcion,
                                                LocalDate fecha, Long siembraId) {
        return registrarActividad(tipo, descripcion, fecha, siembraId, null);
    }
    List<ActividadCultivo> listarActividadesPorSiembra(Long siembraId);
    ActividadCultivo buscarActividadPorId(Long id);
    /** No toca el costo */
    ActividadCultivo actualizarActividad(Long id, String tipo, String descripcion, LocalDate fecha);
    /** costo null = sin costo */
    ActividadCultivo actualizarActividad(Long id, String tipo, String descripcion, LocalDate fecha, Double costo);
    void eliminarActividad(Long id);
}