package com.agroruta.worker.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class Pago {

    private Long id;
    private Nomina nomina;
    private LocalDate fechaPago;
    private BigDecimal monto;
    private MetodoPago metodoPago;
    private String comprobante;
    private String observaciones;

    public Pago(Long id, Nomina nomina, LocalDate fechaPago,
                BigDecimal monto, MetodoPago metodoPago, String comprobante) {
        this.id = id;
        this.nomina = nomina;
        this.fechaPago = fechaPago;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.comprobante = comprobante;
    }

    public boolean montoEsCorrecto() {
        return this.monto.compareTo(this.nomina.getValorTotal()) == 0;
    }
}