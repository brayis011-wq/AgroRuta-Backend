package com.agroruta.crop.application.ports.in;

import com.agroruta.crop.domain.Cosecha;
import java.time.LocalDate;
import java.util.List;

public interface CosechaUseCase {
    /** precioKg: precio de venta por kilo (opcional) */
    Cosecha registrarCosecha(LocalDate fecha, Double cantidadKg,
                             String calidad, String observaciones, Long siembraId, Double precioKg);

    default Cosecha registrarCosecha(LocalDate fecha, Double cantidadKg,
                                     String calidad, String observaciones, Long siembraId) {
        return registrarCosecha(fecha, cantidadKg, calidad, observaciones, siembraId, null);
    }
    List<Cosecha> listarCosechasPorSiembra(Long siembraId);
    Double totalKgCosechado(Long siembraId);
    Cosecha buscarCosechaPorId(Long id);
    /** No toca el precio */
    Cosecha actualizarCosecha(Long id, LocalDate fecha, Double cantidadKg,
                              String calidad, String observaciones);
    /** precioKg null = sin precio */
    Cosecha actualizarCosecha(Long id, LocalDate fecha, Double cantidadKg,
                              String calidad, String observaciones, Double precioKg);
    void eliminarCosecha(Long id);
}