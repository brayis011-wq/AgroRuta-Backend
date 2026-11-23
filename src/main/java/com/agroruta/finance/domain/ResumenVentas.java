package com.agroruta.finance.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cuánto te deben: lo pendiente, lo que ya pasó la fecha de pago y lo vendido este mes.
 *
 * @param compradores los que deben algo, del que más debe al que menos
 */
public record ResumenVentas(BigDecimal porCobrar, BigDecimal vencido, int ventasPendientes, int ventasVencidas,
                            BigDecimal vendidoMes, BigDecimal cobradoMes, List<Deudor> compradores) {

    public record Deudor(String comprador, BigDecimal saldo, int ventas, boolean conVencidas) {}
}
