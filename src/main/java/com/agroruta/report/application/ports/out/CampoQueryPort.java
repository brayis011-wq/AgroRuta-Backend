package com.agroruta.report.application.ports.out;

import com.agroruta.report.domain.AplicacionCampo;
import com.agroruta.report.domain.GastosGenerales;
import com.agroruta.report.domain.Periodo;

import java.util.List;

/** Datos del campo para el cuaderno de campo y la ganancia real. */
public interface CampoQueryPort {

    /**
     * Fumigaciones y fertilizaciones del agricultor en el periodo, de la más antigua a la más reciente.
     * agricultorId null: las de todos (Administrador). fincaId opcional.
     */
    List<AplicacionCampo> aplicaciones(Long agricultorId, Long fincaId, Periodo periodo);

    /** Nombre de la finca (para el encabezado del cuaderno); null si no existe */
    String nombreFinca(Long fincaId);

    /** Gastos generales de las fincas en el periodo, por categoría */
    GastosGenerales gastosGenerales(Long agricultorId, Periodo periodo);
}
