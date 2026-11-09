package com.agroruta.inventory.domain;

import com.agroruta.agriculturalinput.domain.MeasurementUnit;

/** Lo que el inventario necesita saber de un insumo del catálogo. */
public record InsumoCatalogo(Long id, String nombre, String tipo, MeasurementUnit unidadSugerida, boolean activo) {

    public UnidadBase unidadBase() {
        return UnidadBase.de(unidadSugerida);
    }
}
