package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.TipoContrato;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Reemplaza todos los campos (TrabajadorService los asigna tal cual), por eso son obligatorios. */
public class TrabajadorUpdateRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    public String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar 100 caracteres")
    public String apellido;

    @Pattern(regexp = "^$|^[0-9+()\\- ]{7,20}$", message = "El teléfono no es válido")
    public String telefono;

    @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
    public String direccion;

    @NotNull(message = "El tipo de contrato es obligatorio")
    public TipoContrato tipoContrato;
}
