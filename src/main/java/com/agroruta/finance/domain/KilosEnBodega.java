package com.agroruta.finance.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * La bodega de la cosecha de una siembra: lo recogido menos lo vendido. Vender es tomar kilos de aquí
 * y ponerles precio.
 */
public record KilosEnBodega(
        Long siembraId,
        String cultivo,
        String variedad,
        String lote,
        String finca,
        BigDecimal kgCosechados,
        BigDecimal kgVendidos,
        LocalDate ultimaCosecha,
        boolean finalizada) {

    /** Lo que queda por vender (nunca negativo: si se vendió de más, queda en cero) */
    /** Se vendió más de lo cosechado (ventas de antes de registrar la cosecha): cuánto falta por anotar */
    @com.fasterxml.jackson.annotation.JsonProperty("kgSinCosecha")
    public BigDecimal kgSinCosecha() {
        BigDecimal resta = kgVendidos.subtract(kgCosechados).setScale(2, RoundingMode.HALF_UP);
        return resta.signum() > 0 ? resta : BigDecimal.ZERO.setScale(2);
    }

    @com.fasterxml.jackson.annotation.JsonProperty("kgDisponibles")
    public BigDecimal kgDisponibles() {
        BigDecimal resta = kgCosechados.subtract(kgVendidos).setScale(2, RoundingMode.HALF_UP);
        return resta.signum() < 0 ? BigDecimal.ZERO.setScale(2) : resta;
    }
}
