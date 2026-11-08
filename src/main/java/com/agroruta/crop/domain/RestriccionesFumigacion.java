package com.agroruta.crop.domain;

/**
 * Datos de la etiqueta y del gasto de un producto aplicado, que se guardan con cada fumigación:
 * días de carencia (no cosechar), horas de reentrada (no entrar al lote), costo del producto (COP)
 * y cantidad total de producto gastada (lo que se descuenta de la bodega de insumos).
 * Null = no se conoce.
 *
 * @param cantidadUsada  producto gastado en total (la dosis es una concentración, p. ej. 500 g por 200 L)
 * @param unidadCantidad LITROS, ML, KG o GRAMOS
 */
public record RestriccionesFumigacion(Integer diasCarencia, Integer reentradaHoras, Double costo,
                                      Double cantidadUsada, String unidadCantidad) {

    public static final RestriccionesFumigacion NINGUNA = new RestriccionesFumigacion(null, null, null);

    public RestriccionesFumigacion(Integer diasCarencia, Integer reentradaHoras, Double costo) {
        this(diasCarencia, reentradaHoras, costo, null, null);
    }

    public RestriccionesFumigacion(Integer diasCarencia, Integer reentradaHoras) {
        this(diasCarencia, reentradaHoras, null);
    }

    public boolean tieneCantidadUsada() {
        return cantidadUsada != null && cantidadUsada > 0 && unidadCantidad != null && !unidadCantidad.isBlank();
    }
}
