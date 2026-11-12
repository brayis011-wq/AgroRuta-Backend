package com.agroruta.crop.application.ports.in;

import com.agroruta.crop.domain.Cosecha;

import java.time.LocalDate;
import java.util.List;

public interface CosechaUseCase {

    /**
     * precioKg: precio de venta por kilo (opcional).
     * confirmarCarencia: si la fecha cae en la carencia de una fumigación, sin esto se rechaza
     * (HARVEST_IN_WITHDRAWAL); con esto se guarda marcada como "en carencia".
     */
    Cosecha registrarCosecha(LocalDate fecha, Double cantidadKg, String calidad, String observaciones,
                             Long siembraId, Double precioKg, boolean confirmarCarencia);

    default Cosecha registrarCosecha(LocalDate fecha, Double cantidadKg,
                                     String calidad, String observaciones, Long siembraId, Double precioKg) {
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
    Cosecha actualizarCosecha(Long id, LocalDate fecha, Double cantidadKg,
                              String calidad, String observaciones, Double precioKg, boolean confirmarCarencia);

    default Cosecha actualizarCosecha(Long id, LocalDate fecha, Double cantidadKg,
                                      String calidad, String observaciones, Double precioKg) {
        return actualizarCosecha(id, fecha, cantidadKg, calidad, observaciones, precioKg, false);
    }

    void eliminarCosecha(Long id);
}
