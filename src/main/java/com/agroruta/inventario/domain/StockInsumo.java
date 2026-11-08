package com.agroruta.inventario.domain;

import java.time.LocalDate;

/**
 * Cómo está un insumo en la bodega del agricultor. Cantidades en la unidad base (ml o g).
 *
 * @param costoPromedio lo que cuesta cada ml o g según las compras registradas (null si ninguna tiene precio)
 * @param valorStock    lo que vale lo que queda, a costo promedio (null si no hay costo promedio)
 * @param stockMinimo   cantidad a partir de la cual avisar (null si no se definió)
 */
public record StockInsumo(
        Long insumoId,
        String nombre,
        String tipo,
        UnidadBase unidadBase,
        double cantidad,
        Double stockMinimo,
        Double costoPromedio,
        Double valorStock,
        EstadoStock estado,
        LocalDate ultimoMovimiento
) {}
