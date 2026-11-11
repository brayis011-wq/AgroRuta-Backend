package com.agroruta.worker.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lo que dice el comprobante de un pago: quién pagó, a quién, cuánto y por qué período.
 * {@code codigo} es la firma con la que cualquiera puede comprobar que el comprobante es auténtico
 * (va en el QR, dentro de {@code urlVerificacion}).
 */
public record ComprobantePago(
        Long pagoId,
        /** "CP-000123" */
        String numero,
        LocalDate fechaPago,
        BigDecimal monto,
        MetodoPago metodoPago,
        /** Referencia que escribió el agricultor (número de transferencia, de cheque...) */
        String referencia,
        String observaciones,
        String trabajador,
        String cedula,
        String empleador,
        LocalDate periodoInicio,
        LocalDate periodoFin,
        int jornales,
        String codigo,
        String urlVerificacion
) {
    public static String numeroDe(Long pagoId) {
        return String.format("CP-%06d", pagoId);
    }

    /** "•••• 120": la cédula no se muestra completa a quien solo verifica el comprobante */
    public String cedulaOculta() {
        if (cedula == null || cedula.isBlank()) return null;
        String limpia = cedula.trim();
        return "•••• " + limpia.substring(Math.max(0, limpia.length() - 3));
    }
}
