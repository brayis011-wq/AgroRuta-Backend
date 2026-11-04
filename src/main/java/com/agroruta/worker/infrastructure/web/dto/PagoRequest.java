package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.MetodoPago;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class PagoRequest {

    @NotNull(message = "La nómina es obligatoria")
    @Positive(message = "La nómina no es válida")
    public Long nominaId;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    @Digits(integer = 12, fraction = 2, message = "El monto admite máximo 2 decimales")
    public BigDecimal monto;

    @NotNull(message = "El método de pago es obligatorio")
    public MetodoPago metodoPago;

    @Size(max = 100, message = "El comprobante no puede superar 100 caracteres")
    public String comprobante;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    public String observaciones;
}
