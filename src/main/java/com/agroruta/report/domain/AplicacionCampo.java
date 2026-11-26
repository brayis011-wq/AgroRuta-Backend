package com.agroruta.report.domain;

import java.time.LocalDate;

/**
 * Una aplicación de producto (fumigación o fertilización) para el cuaderno de campo que piden
 * las certificaciones y algunos compradores: qué se aplicó, dónde, cuánto y hasta cuándo no se cosecha.
 */
public record AplicacionCampo(
        LocalDate fecha,
        String finca,
        String lote,
        String cultivo,
        String producto,
        /** Tipo del producto del catálogo (FUNGICIDA, FERTILIZANTE_FOLIAR…); null si se escribió a mano */
        String tipoProducto,
        Double dosis,
        String unidadDosis,
        Double areaAplicada,
        Double cantidadUsada,
        String unidadCantidad,
        Integer diasCarencia,
        Integer reentradaHoras,
        String observaciones) {
    /** Primer día en que se puede volver a cosechar; null sin carencia */
    public LocalDate finCarencia() {
        return fecha == null || diasCarencia == null || diasCarencia <= 0 ? null : fecha.plusDays(diasCarencia);
    }
}
