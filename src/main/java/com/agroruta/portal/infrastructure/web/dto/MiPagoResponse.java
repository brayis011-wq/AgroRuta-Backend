package com.agroruta.portal.infrastructure.web.dto;

import com.agroruta.worker.domain.Pago;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MiPagoResponse(Long id, LocalDate fechaPago, BigDecimal monto, String metodoPago,
                             String comprobante, LocalDate periodoInicio, LocalDate periodoFin) {

    public static MiPagoResponse from(Pago p) {
        return new MiPagoResponse(p.getId(), p.getFechaPago(), p.getMonto(),
                p.getMetodoPago() != null ? p.getMetodoPago().name() : null, p.getComprobante(),
                p.getNomina() != null ? p.getNomina().getPeriodoInicio() : null,
                p.getNomina() != null ? p.getNomina().getPeriodoFin() : null);
    }
}
