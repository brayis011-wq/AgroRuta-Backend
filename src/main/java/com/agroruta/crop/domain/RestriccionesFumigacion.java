package com.agroruta.crop.domain;

/**
 * Restricciones de un producto aplicado, copiadas de la etiqueta o del catálogo al registrar:
 * días de carencia (no cosechar) y horas de reentrada (no entrar al lote).
 * Null = no se conoce.
 */
public record RestriccionesFumigacion(Integer diasCarencia, Integer reentradaHoras) {
    public static final RestriccionesFumigacion NINGUNA = new RestriccionesFumigacion(null, null);
}
