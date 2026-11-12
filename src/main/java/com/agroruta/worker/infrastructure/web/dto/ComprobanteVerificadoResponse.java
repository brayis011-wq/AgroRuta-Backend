package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.ComprobantePago;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lo que ve quien escanea el QR de un comprobante: lo justo para compararlo con el papel. */
public record ComprobanteVerificadoResponse(String numero, LocalDate fechaPago, BigDecimal monto, String metodoPago,
                                            String trabajador, String cedula, String empleador,
                                            LocalDate periodoInicio, LocalDate periodoFin) {

    public static ComprobanteVerificadoResponse from(ComprobantePago c) {
        return new ComprobanteVerificadoResponse(c.numero(), c.fechaPago(), c.monto(),
                c.metodoPago() != null ? c.metodoPago().name() : null,
                c.trabajador(), c.cedulaOculta(), c.empleador(), c.periodoInicio(), c.periodoFin());
    }
}
