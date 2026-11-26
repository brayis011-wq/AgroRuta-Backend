package com.agroruta.crop.application.ports.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Lo que las fumigaciones le piden a la bodega de insumos: descontar el producto gastado
 * y saber cuánto costó a precio promedio de compra.
 */
public interface InventarioPort {

    /**
     * Registra (o corrige) el gasto de producto de una fumigación.
     *
     * @param unidad LITROS, ML, KG o GRAMOS
     * @return lo que costó a precio promedio de compra, si hay compras con precio
     */
    Optional<BigDecimal> registrarUso(
            Long fumigacionId, Long agricultorId, Long insumoId, double cantidad, String unidad, LocalDate fecha);

    /** Devuelve a la bodega el producto de una fumigación (se borró o ya no usa un insumo del catálogo) */
    void quitarUso(Long fumigacionId);

    /** Igual que registrarUso, para una actividad (abonado, fertilización al suelo...) */
    Optional<BigDecimal> registrarUsoDeActividad(
            Long actividadId, Long agricultorId, Long insumoId, double cantidad, String unidad, LocalDate fecha);

    /** Devuelve a la bodega el producto de una actividad */
    void quitarUsoDeActividad(Long actividadId);
}
