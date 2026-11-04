package com.agroruta.portal.infrastructure.web.dto;

import com.agroruta.portal.domain.EstadoPortal;
import com.agroruta.worker.domain.Trabajador;

import java.time.LocalDate;

public record EstadoPortalResponse(String estado, MisDatos trabajador) {

    /** Datos del trabajador que puede ver la propia persona. */
    public record MisDatos(String nombreCompleto, String cedula, String cargo, String estado,
                           String tipoContrato, LocalDate fechaIngreso, String empleador) {}

    public static EstadoPortalResponse from(EstadoPortal e) {
        Trabajador t = e.trabajador();
        MisDatos datos = t == null ? null : new MisDatos(
                t.getNombreCompleto(), t.getCedula(),
                t.getCargo() != null ? t.getCargo().getNombre() : null,
                t.getEstado() != null ? t.getEstado().name() : null,
                t.getTipoContrato() != null ? t.getTipoContrato().name() : null,
                t.getFechaIngreso(), e.empleador());
        return new EstadoPortalResponse(e.estado(), datos);
    }
}
