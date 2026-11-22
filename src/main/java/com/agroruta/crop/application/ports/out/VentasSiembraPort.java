package com.agroruta.crop.application.ports.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Lo que la siembra le pide a Finanzas: sus ventas, que son los ingresos de la temporada. */
public interface VentasSiembraPort {

    /** kg puede ser null (venta por un total, sin kilos) */
    record VentaDeSiembra(LocalDate fecha, BigDecimal kg, BigDecimal total) {}

    List<VentaDeSiembra> ventasDeSiembra(Long siembraId);
}
