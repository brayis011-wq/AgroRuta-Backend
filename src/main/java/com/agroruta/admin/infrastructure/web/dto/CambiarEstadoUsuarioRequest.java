package com.agroruta.admin.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoUsuarioRequest(
        @NotNull(message = "Indica si la cuenta queda activa o no")
        Boolean activo
) {}
