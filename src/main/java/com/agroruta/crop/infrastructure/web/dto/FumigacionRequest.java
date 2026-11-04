package com.agroruta.crop.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FumigacionRequest {

    @NotNull(message = "La fecha es obligatoria")
    @PastOrPresent(message = "La fecha de fumigación no puede ser futura")
    private LocalDate fecha;

    @NotBlank(message = "El producto es obligatorio")
    @Size(max = 150, message = "El producto no puede superar 150 caracteres")
    private String producto;

    // Opcional: solo viene si el producto se eligió del catálogo.
    private Long agriculturalInputId;

    @NotNull(message = "La dosis es obligatoria")
    @Positive(message = "La dosis debe ser mayor a 0")
    private Double dosis;

    // El valor exacto (LITROS, ML, GRAMOS, KG) lo valida FumigacionService.
    @NotBlank(message = "La unidad de medida es obligatoria")
    private String unidadMedida;

    @NotNull(message = "El área aplicada es obligatoria")
    @Positive(message = "El área aplicada debe ser mayor a 0")
    private Double areaAplicada;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    private String observaciones;

    @NotNull(message = "La siembra es obligatoria")
    @Positive(message = "La siembra no es válida")
    private Long siembraId;
}
