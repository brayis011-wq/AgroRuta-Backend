package com.agroruta.crop.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

/** DISPONIBLE o EN_DESCANSO (EN_CULTIVO se pone solo al sembrar) */
public record CambiarEstadoLoteRequest(
        @NotBlank(message = "El estado es obligatorio") String estado) {}
