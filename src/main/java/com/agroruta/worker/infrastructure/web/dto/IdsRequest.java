package com.agroruta.worker.infrastructure.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Varias nóminas a la vez. */
public class IdsRequest {

    @NotEmpty(message = "Elige al menos una nómina")
    @Size(max = 200, message = "Demasiadas nóminas a la vez")
    public List<@NotNull Long> ids;
}
