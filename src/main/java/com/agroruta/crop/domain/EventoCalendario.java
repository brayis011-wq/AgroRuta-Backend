package com.agroruta.crop.domain;

import java.time.LocalDate;

/**
 * Algo que pasó o que viene en un cultivo, para el calendario.
 *
 * @param tipo    SIEMBRA, ETAPA, ACTIVIDAD, FUMIGACION, COSECHA, FIN_CARENCIA, FIN_REENTRADA,
 *                FUMIGACION_SUGERIDA, PROXIMA_ETAPA, COSECHA_ESTIMADA
 * @param estado  HECHO, PENDIENTE (programado o restricción en curso), ESTIMADO (calculado con
 *                el historial) o ATRASADO (debió pasar y no se ha registrado)
 * @param titulo  texto principal: producto, tipo de actividad, etapa, kilos...
 * @param detalle texto secundario (puede ser null)
 * @param refId   id del registro (actividad, fumigación, cosecha) cuando aplica
 */
public record EventoCalendario(
        LocalDate fecha,
        String tipo,
        String estado,
        String titulo,
        String detalle,
        Long siembraId,
        Long loteId,
        String loteNombre,
        String cultivo,
        Long refId
) {}
