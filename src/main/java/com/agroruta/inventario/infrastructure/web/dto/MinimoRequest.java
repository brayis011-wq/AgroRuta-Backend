package com.agroruta.inventario.infrastructure.web.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/** Cantidad a partir de la cual avisar; minimo = null quita el aviso */
@Data
public class MinimoRequest {

    @PositiveOrZero(message = "El mínimo no puede ser negativo")
    private Double minimo;

    private String unidad;
}
