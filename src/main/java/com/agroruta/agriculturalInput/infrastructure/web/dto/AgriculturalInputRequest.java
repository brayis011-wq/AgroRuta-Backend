package com.agroruta.agriculturalInput.infrastructure.web.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AgriculturalInputRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @NotBlank(message = "El tipo es obligatorio")
    @Pattern(
            regexp  = "FUNGICIDA|INSECTICIDA|HERBICIDA|FERTILIZANTE_FOLIAR",
            message = "Tipo inválido. Valores permitidos: FUNGICIDA, INSECTICIDA, HERBICIDA, FERTILIZANTE_FOLIAR"
    )
    private String tipo;

    @NotBlank(message = "La unidad sugerida es obligatoria")
    @Pattern(
            regexp  = "LITROS|ML|GRAMOS|KG",
            message = "Unidad inválida. Valores permitidos: LITROS, ML, GRAMOS, KG"
    )
    private String unidadSugerida;

    @NotNull(message = "La dosis sugerida es obligatoria")
    @DecimalMin(value = "0.01", message = "La dosis debe ser mayor a 0")
    private Double dosisSugerida;

    @NotNull(message = "Las horas de reentrada son obligatorias")
    @Min(value = 0, message = "Las horas de reentrada no pueden ser negativas")
    private Integer reentradaHoras;
}