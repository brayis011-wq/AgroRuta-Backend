package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.MetodoPago;
import com.agroruta.worker.domain.Pago;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PagoResponse {
    public Long id;
    public Long nominaId;
    public Long trabajadorId;
    public String trabajadorNombre;
    public LocalDate fechaPago;
    public BigDecimal monto;
    public MetodoPago metodoPago;
    public String comprobante;
    public String observaciones;

    public static PagoResponse from(Pago p) {
        PagoResponse r = new PagoResponse();
        r.id = p.getId();
        r.nominaId = p.getNomina().getId();
        r.trabajadorId = p.getNomina().getTrabajador().getId();
        r.trabajadorNombre = p.getNomina().getTrabajador().getNombreCompleto();
        r.fechaPago = p.getFechaPago();
        r.monto = p.getMonto();
        r.metodoPago = p.getMetodoPago();
        r.comprobante = p.getComprobante();
        r.observaciones = p.getObservaciones();
        return r;
    }
}