package com.agroruta.crop.domain;

import java.util.List;

/**
 * Cuánto costó y cuánto dejó una temporada (pesos colombianos).
 * Mano de obra = jornales registrados a la siembra; insumos = costo de las fumigaciones;
 * otros = costo de materiales de las actividades; ingresos = lo vendido de la siembra (Ventas) o, si no tiene
 * ventas, kilos × precio de cada cosecha (como se registraba antes).
 */
public record CostosSiembra(
        Long siembraId,
        double manoObra,
        int jornales,
        double insumos,
        double otros,
        double costoTotal,
        double ingresos,
        double kgTotal,
        /** Kilos de cosechas que tienen precio */
        double kgConPrecio,
        /** null si no hay precios */
        Double precioPromedioKg,
        /** Ingresos − costos; null si no hay ningún precio de venta */
        Double ganancia,
        /** Costo total ÷ kilos cosechados; null sin cosechas */
        Double costoPorKg,
        List<CostoTrabajador> trabajadores,
        List<CostoMes> meses,
        int fumigacionesSinCosto,
        int cosechasSinPrecio,
        /** De dónde salen los ingresos: VENTAS, COSECHAS (precio anotado en la cosecha) o null si no hay */
        String fuenteIngresos,
        /** Kilos vendidos de la siembra (de sus ventas) */
        double kgVendidos
) {
    public record CostoTrabajador(Long trabajadorId, String nombre, int jornales, double valor) {}

    /** mes: "2026-10" */
    public record CostoMes(String mes, double manoObra, double insumos, double otros, double ingresos) {}
}
