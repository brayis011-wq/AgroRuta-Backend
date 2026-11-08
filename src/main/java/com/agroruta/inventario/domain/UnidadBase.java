package com.agroruta.inventario.domain;

import com.agroruta.agriculturalInput.domain.MeasurementUnit;
import com.agroruta.shared.exception.BusinessException;

/**
 * El stock se guarda siempre en la unidad más pequeña de su tipo: mililitros para líquidos y
 * gramos para sólidos. Así se puede comprar en litros o kilos y gastar en mililitros o gramos.
 */
public enum UnidadBase {
    ML, GRAMOS;

    public static UnidadBase de(MeasurementUnit unidad) {
        if (unidad == null) throw new BusinessException("La unidad es obligatoria.");
        return switch (unidad) {
            case LITROS, ML -> ML;
            case KG, GRAMOS -> GRAMOS;
        };
    }

    /** Convierte una cantidad a la unidad base (litros → ml, kilos → g). */
    public static double aBase(double cantidad, MeasurementUnit unidad) {
        if (unidad == null) throw new BusinessException("La unidad es obligatoria.");
        return switch (unidad) {
            case LITROS, KG -> cantidad * 1000;
            case ML, GRAMOS -> cantidad;
        };
    }

    /**
     * Convierte una cantidad a la unidad base de un insumo, validando que la unidad sea del mismo
     * tipo (no se pueden sumar litros a un producto que se lleva en gramos).
     */
    public static double aBaseDe(double cantidad, MeasurementUnit unidad, UnidadBase delInsumo, String insumo) {
        if (de(unidad) != delInsumo) {
            throw new BusinessException(delInsumo == ML
                    ? insumo + " se mide en litros o mililitros."
                    : insumo + " se mide en kilos o gramos.");
        }
        return aBase(cantidad, unidad);
    }
}
