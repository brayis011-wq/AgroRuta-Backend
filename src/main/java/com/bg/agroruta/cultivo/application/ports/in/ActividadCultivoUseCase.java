package com.bg.agroruta.cultivo.application.ports.in;

import com.bg.agroruta.cultivo.domain.ActividadCultivo;
import java.util.List;

public interface ActividadCultivoUseCase {
    ActividadCultivo registrarActividad(ActividadCultivo actividad);
    List<ActividadCultivo> listarActividadesPorSiembra(Long siembraId);
}