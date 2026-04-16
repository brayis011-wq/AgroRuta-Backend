package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.MetodoPago;

import java.math.BigDecimal;

public class PagoRequest {
    public Long nominaId;
    public BigDecimal monto;
    public MetodoPago metodoPago;
    public String comprobante;
    public String observaciones;
}
