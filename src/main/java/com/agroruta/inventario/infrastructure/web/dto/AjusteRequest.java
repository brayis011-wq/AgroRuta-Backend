package com.agroruta.inventario.infrastructure.web.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Conteo físico: cuánto hay de verdad en la bodega */
@Data
public class AjusteRequest {

    @NotNull(message = "El insumo es obligatorio")
    private Long insumoId;

    @NotNull(message = "La cantidad contada es obligatoria")
    @PositiveOrZero(message = "La cantidad contada no puede ser negativa")
    private Double cantidadReal;

    @NotBlank(message = "La unidad es obligatoria")
    private String unidad;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    private String observaciones;
}
