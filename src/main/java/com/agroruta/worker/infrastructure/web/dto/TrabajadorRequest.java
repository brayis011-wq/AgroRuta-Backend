package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.TipoContrato;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class TrabajadorRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    public String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar 100 caracteres")
    public String apellido;

    // Admite cédula, cédula de extranjería o pasaporte (letras, números, puntos y guiones).
    @NotBlank(message = "La cédula es obligatoria")
    @Pattern(regexp = "^[0-9A-Za-z.\\-]{5,20}$",
            message = "La cédula debe tener entre 5 y 20 caracteres (números, letras, puntos o guiones)")
    public String cedula;

    @Pattern(regexp = "^$|^[0-9+()\\- ]{7,20}$", message = "El teléfono no es válido")
    public String telefono;

    @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
    public String direccion;

    @NotNull(message = "La fecha de ingreso es obligatoria")
    public LocalDate fechaIngreso;

    @NotNull(message = "El tipo de contrato es obligatorio")
    public TipoContrato tipoContrato;

    @NotNull(message = "El cargo es obligatorio")
    @Positive(message = "El cargo no es válido")
    public Long cargoId;
}
