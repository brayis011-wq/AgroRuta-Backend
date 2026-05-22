package com.agroruta.crop.application.ports.in;

import com.agroruta.crop.domain.ActividadCultivo;
import java.time.LocalDate;
import java.util.List;

public interface ActividadCultivoUseCase {
    ActividadCultivo registrarActividad(String tipo, String descripcion,
                                        LocalDate fecha, Long siembraId);
    List<ActividadCultivo> listarActividadesPorSiembra(Long siembraId);
}