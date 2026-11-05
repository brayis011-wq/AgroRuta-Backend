package com.agroruta.crop.domain;

/**
 * Datos de la etiqueta y del gasto de un producto aplicado, que se guardan con cada fumigación:
 * días de carencia (no cosechar), horas de reentrada (no entrar al lote) y costo del producto (COP).
 * Null = no se conoce.
 */
public record RestriccionesFumigacion(Integer diasCarencia, Integer reentradaHoras, Double costo) {
    public static final RestriccionesFumigacion NINGUNA = new RestriccionesFumigacion(null, null, null);

    public RestriccionesFumigacion(Integer diasCarencia, Integer reentradaHoras) {
        this(diasCarencia, reentradaHoras, null);
    }
}
