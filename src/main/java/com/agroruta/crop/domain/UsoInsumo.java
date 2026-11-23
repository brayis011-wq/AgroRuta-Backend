package com.agroruta.crop.domain;

/**
 * El insumo que gastó una actividad (abonado, fertilización al suelo...).
 *
 * @param insumoId  producto de la bodega (null = escrito a mano: no se descuenta)
 * @param producto  nombre del producto
 * @param cantidad  cantidad total usada (sin ella no se descuenta de la bodega)
 * @param unidad    LITROS, ML, KG o GRAMOS
 */
public record UsoInsumo(Long insumoId, String producto, Double cantidad, String unidad) {

    /** Se descuenta de la bodega: producto de la bodega con cantidad */
    public boolean descuentaDeBodega() {
        return insumoId != null && cantidad != null && cantidad > 0 && unidad != null;
    }
}
