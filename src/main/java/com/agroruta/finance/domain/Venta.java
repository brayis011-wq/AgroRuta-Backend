package com.agroruta.finance.domain;

import com.agroruta.shared.exception.DatoInvalidoException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que se le vendió a un comprador y cuánto ha pagado. El saldo es lo que todavía debe.
 *
 * @param siembraNombre solo al leer ("Café Castillo · Lote 2")
 * @param abonado       suma de los abonos
 * @param abonos        solo al leer una venta (en las listas viene vacío)
 */
public record Venta(
        Long id,
        Long siembraId,
        String siembraNombre,
        String comprador,
        LocalDate fecha,
        String producto,
        BigDecimal cantidadKg,
        BigDecimal precioKg,
        BigDecimal total,
        LocalDate vence,
        String observaciones,
        BigDecimal abonado,
        List<Abono> abonos) {

    public enum Estado {
        PENDIENTE,
        VENCIDA,
        PAGADA
    }

    public record Abono(
            Long id, Long ventaId, LocalDate fecha, BigDecimal monto, String metodo, String observaciones) {}

    public BigDecimal saldo() {
        BigDecimal a = abonado == null ? BigDecimal.ZERO : abonado;
        BigDecimal s = total.subtract(a);
        return s.signum() < 0 ? BigDecimal.ZERO : s;
    }

    public Estado estado(LocalDate hoy) {
        if (saldo().signum() == 0) return Estado.PAGADA;
        return vence != null && vence.isBefore(hoy) ? Estado.VENCIDA : Estado.PENDIENTE;
    }

    /**
     * Revisa los datos de una venta nueva o editada. Si vienen kilos y precio, el total es kilos × precio;
     * si no, el total es obligatorio.
     */
    public static Venta nueva(
            Long id,
            Long siembraId,
            String comprador,
            LocalDate fecha,
            String producto,
            BigDecimal cantidadKg,
            BigDecimal precioKg,
            BigDecimal total,
            LocalDate vence,
            String observaciones) {
        String c = comprador == null ? "" : comprador.trim();
        String p = producto == null ? "" : producto.trim();
        if (c.isEmpty()) throw new DatoInvalidoException("Escribe a quién le vendiste.");
        if (p.isEmpty()) throw new DatoInvalidoException("Escribe qué vendiste.");
        if (fecha == null) throw new DatoInvalidoException("Indica la fecha de la venta.");
        if (vence != null && vence.isBefore(fecha)) {
            throw new DatoInvalidoException("La fecha de pago no puede ser anterior a la venta.");
        }
        BigDecimal t = total;
        if (cantidadKg != null && precioKg != null) {
            t = cantidadKg.multiply(precioKg).setScale(2, RoundingMode.HALF_UP);
        }
        if (t == null || t.signum() <= 0)
            throw new DatoInvalidoException("Indica el valor total o los kilos y el precio por kilo.");
        String obs = observaciones == null || observaciones.isBlank() ? null : observaciones.trim();
        return new Venta(
                id,
                siembraId,
                null,
                c,
                fecha,
                p,
                cantidadKg,
                precioKg,
                t.setScale(2, RoundingMode.HALF_UP),
                vence,
                obs,
                BigDecimal.ZERO,
                List.of());
    }
}
