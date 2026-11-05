package com.agroruta.crop.application.ports.in;

import com.agroruta.crop.domain.AlertaCultivo;

import java.time.LocalDate;
import java.util.List;

public interface AlertasCultivoUseCase {

    /** Alertas de hoy de todas las siembras activas del agricultor */
    List<AlertaCultivo> alertasDelDia(Long agricultorId, LocalDate hoy);
}
