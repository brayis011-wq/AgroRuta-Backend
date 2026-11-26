package com.agroruta.finance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un gasto de la finca que no es de una siembra en particular: transporte, herramientas,
 * servicios… Entra en la rentabilidad para que la ganancia sea la real.
 *
 * @param fincaNombre solo al leer (para las listas)
 */
public record GastoFinca(
        Long id,
        Long fincaId,
        String fincaNombre,
        LocalDate fecha,
        Categoria categoria,
        String descripcion,
        BigDecimal monto) {

    public enum Categoria {
        TRANSPORTE,
        HERRAMIENTAS,
        SERVICIOS,
        MANTENIMIENTO,
        ADMINISTRACION,
        IMPUESTOS,
        OTRO
    }
}
