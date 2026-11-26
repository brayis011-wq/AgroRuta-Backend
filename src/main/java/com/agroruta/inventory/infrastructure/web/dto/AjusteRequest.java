package com.agroruta.inventory.infrastructure.web.dto;

import jakarta.validation.constraints.*;

/** Conteo físico: cuánto hay de verdad en la bodega */
public record AjusteRequest(
        @NotNull(message = "El insumo es obligatorio") Long insumoId,

        @NotNull(message = "La cantidad contada es obligatoria")
        @PositiveOrZero(message = "La cantidad contada no puede ser negativa")
        Double cantidadReal,

        @NotBlank(message = "La unidad es obligatoria") String unidad,

        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        String observaciones) {}
