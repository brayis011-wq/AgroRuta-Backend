package com.agroruta.crop.application.ports.in;

import com.agroruta.crop.domain.ActividadCultivo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ActividadCultivoUseCase {
    /** costo: materiales o servicios de la actividad (opcional) */
    default ActividadCultivo registrarActividad(
            String tipo, String descripcion, LocalDate fecha, Long siembraId, BigDecimal costo) {
        return registrarActividad(tipo, descripcion, fecha, siembraId, costo, null);
    }

    /**
     * insumo: lo que se gastó (abonado...). Si es de la bodega y trae la cantidad, se descuenta de ella y,
     * sin costo escrito, el costo se calcula a precio promedio de compra.
     */
    ActividadCultivo registrarActividad(
            String tipo,
            String descripcion,
            LocalDate fecha,
            Long siembraId,
            BigDecimal costo,
            com.agroruta.crop.domain.UsoInsumo insumo);

    default ActividadCultivo registrarActividad(String tipo, String descripcion, LocalDate fecha, Long siembraId) {
        return registrarActividad(tipo, descripcion, fecha, siembraId, null);
    }

    List<ActividadCultivo> listarActividadesPorSiembra(Long siembraId);

    ActividadCultivo buscarActividadPorId(Long id);
    /** No toca el costo */
    ActividadCultivo actualizarActividad(Long id, String tipo, String descripcion, LocalDate fecha);
    /** costo null = sin costo */
    default ActividadCultivo actualizarActividad(
            Long id, String tipo, String descripcion, LocalDate fecha, BigDecimal costo) {
        return actualizarActividad(id, tipo, descripcion, fecha, costo, null);
    }

    /** insumo: como al registrar (null = sin insumo; si tenía, el producto vuelve a la bodega) */
    ActividadCultivo actualizarActividad(
            Long id,
            String tipo,
            String descripcion,
            LocalDate fecha,
            BigDecimal costo,
            com.agroruta.crop.domain.UsoInsumo insumo);

    void eliminarActividad(Long id);
}
