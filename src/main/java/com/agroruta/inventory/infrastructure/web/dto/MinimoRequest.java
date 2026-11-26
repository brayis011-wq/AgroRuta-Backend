package com.agroruta.inventory.infrastructure.web.dto;

import jakarta.validation.constraints.PositiveOrZero;

/** Cantidad a partir de la cual avisar; minimo = null quita el aviso */
public record MinimoRequest(
        @PositiveOrZero(message = "El mínimo no puede ser negativo")
        Double minimo,

        String unidad) {}
