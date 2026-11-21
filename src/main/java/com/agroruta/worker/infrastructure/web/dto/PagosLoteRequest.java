package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.MetodoPago;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Pagar varias nóminas juntas, cada una por su valor total. */
public class PagosLoteRequest {

    @NotEmpty(message = "Elige al menos una nómina")
    @Size(max = 200, message = "Demasiadas nóminas a la vez")
    public List<@NotNull Long> nominaIds;

    @NotNull(message = "El método de pago es obligatorio")
    public MetodoPago metodoPago;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    public String observaciones;

    /** Aprobar antes las que sigan pendientes */
    public boolean aprobarPendientes;
}
