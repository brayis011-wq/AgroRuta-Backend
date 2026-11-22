package com.agroruta.crop.application.ports.in;

import com.agroruta.crop.domain.Cosecha;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

public interface CosechaUseCase {

    /**
     * precioKg: precio de venta por kilo (opcional).
     * confirmarCarencia: si la fecha cae en la carencia de una fumigación, sin esto se rechaza
     * (HARVEST_IN_WITHDRAWAL); con esto se guarda marcada como "en carencia".
     */
    default Cosecha registrarCosecha(LocalDate fecha, Double cantidadKg, String calidad, String observaciones,
                                     Long siembraId, BigDecimal precioKg, boolean confirmarCarencia) {
        return registrarCosecha(fecha, cantidadKg, calidad, observaciones, siembraId, precioKg, confirmarCarencia, null);
    }

    /**
     * recolectores: quién la recogió; a cada uno se le crea su jornal (null = sin recolectores).
     */
    Cosecha registrarCosecha(LocalDate fecha, Double cantidadKg, String calidad, String observaciones,
                             Long siembraId, BigDecimal precioKg, boolean confirmarCarencia,
                             java.util.List<com.agroruta.crop.domain.Recolector> recolectores);

    default Cosecha registrarCosecha(LocalDate fecha, Double cantidadKg,
                                     String calidad, String observaciones, Long siembraId, BigDecimal precioKg) {
        return registrarCosecha(fecha, cantidadKg, calidad, observaciones, siembraId, precioKg, false);
    }

    default Cosecha registrarCosecha(LocalDate fecha, Double cantidadKg,
                                     String calidad, String observaciones, Long siembraId) {
        return registrarCosecha(fecha, cantidadKg, calidad, observaciones, siembraId, null, false);
    }

    List<Cosecha> listarCosechasPorSiembra(Long siembraId);

    Double totalKgCosechado(Long siembraId);

    Cosecha buscarCosechaPorId(Long id);

    /** No toca el precio */
    Cosecha actualizarCosecha(Long id, LocalDate fecha, Double cantidadKg,
                              String calidad, String observaciones);

    /** precioKg null = sin precio. confirmarCarencia: igual que al registrar. */
    default Cosecha actualizarCosecha(Long id, LocalDate fecha, Double cantidadKg,
                                      String calidad, String observaciones, BigDecimal precioKg, boolean confirmarCarencia) {
        return actualizarCosecha(id, fecha, cantidadKg, calidad, observaciones, precioKg, confirmarCarencia, null);
    }

    /** recolectores: la lista nueva de quién la recogió; null = no se tocan (si cambia la fecha, sus jornales la siguen) */
    Cosecha actualizarCosecha(Long id, LocalDate fecha, Double cantidadKg,
                              String calidad, String observaciones, BigDecimal precioKg, boolean confirmarCarencia,
                              java.util.List<com.agroruta.crop.domain.Recolector> recolectores);

    default Cosecha actualizarCosecha(Long id, LocalDate fecha, Double cantidadKg,
                                      String calidad, String observaciones, BigDecimal precioKg) {
        return actualizarCosecha(id, fecha, cantidadKg, calidad, observaciones, precioKg, false);
    }

    void eliminarCosecha(Long id);
}
