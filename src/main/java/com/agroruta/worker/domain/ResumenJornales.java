package com.agroruta.worker.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Totales de los jornales de un trabajador (en un periodo opcional), calculados en la base
 * para que la lista paginada muestre los mismos números que antes mostraba con todos los jornales.
 *
 * @param primeraPendiente fecha del jornal sin liquidar más antiguo (para proponer el periodo de la nómina)
 * @param ultimaPendiente  fecha del jornal sin liquidar más reciente
 */
public record ResumenJornales(long cantidad, BigDecimal valorTotal, long pendientes, BigDecimal valorPendiente,
                              LocalDate primeraPendiente, LocalDate ultimaPendiente) {

    public ResumenJornales {
        if (valorTotal == null) valorTotal = BigDecimal.ZERO;
        if (valorPendiente == null) valorPendiente = BigDecimal.ZERO;
    }
}
