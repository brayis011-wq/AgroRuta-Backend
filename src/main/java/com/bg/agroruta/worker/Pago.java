package com.bg.agroruta.worker;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representa el pago efectivo de una nómina liquidada.
 * Permite llevar el historial de cómo y cuándo se pagó cada nómina.
 */
public class Pago {

    private Long id;
    private Nomina nomina;
    private LocalDate fechaPago;
    private BigDecimal monto;
    private MetodoPago metodoPago;
    private String comprobante;      // Número de referencia, cheque, etc.
    private String observaciones;

    public Pago() {}

    public Pago(Long id, Nomina nomina, LocalDate fechaPago,
                BigDecimal monto, MetodoPago metodoPago, String comprobante) {
        this.id = id;
        this.nomina = nomina;
        this.fechaPago = fechaPago;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.comprobante = comprobante;
    }

    // ── Lógica de dominio ────────────────────────────────────────────────────

    /**
     * Verifica si el monto pagado coincide con el total de la nómina.
     */
    public boolean montoEsCorrecto() {
        return this.monto.compareTo(this.nomina.getValorTotal()) == 0;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Nomina getNomina() { return nomina; }
    public void setNomina(Nomina nomina) { this.nomina = nomina; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { this.metodoPago = metodoPago; }

    public String getComprobante() { return comprobante; }
    public void setComprobante(String comprobante) { this.comprobante = comprobante; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
