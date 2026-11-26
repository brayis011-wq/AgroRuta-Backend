package com.agroruta.agriculturalinput.infrastructure.web.dto;

import jakarta.validation.constraints.*;

public record AgriculturalInputRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @NotBlank(message = "El tipo es obligatorio")
        @Pattern(
                regexp = "FUNGICIDA|INSECTICIDA|HERBICIDA|FERTILIZANTE_FOLIAR",
                message = "Tipo inválido. Valores permitidos: FUNGICIDA, INSECTICIDA, HERBICIDA, FERTILIZANTE_FOLIAR")
        String tipo,

        @NotBlank(message = "La unidad sugerida es obligatoria")
        @Pattern(
                regexp = "LITROS|ML|GRAMOS|KG",
                message = "Unidad inválida. Valores permitidos: LITROS, ML, GRAMOS, KG")
        String unidadSugerida,

        @NotNull(message = "La dosis sugerida es obligatoria")
        @DecimalMin(value = "0.01", message = "La dosis debe ser mayor a 0")
        Double dosisSugerida,

        @NotNull(message = "Las horas de reentrada son obligatorias")
        @Min(value = 0, message = "Las horas de reentrada no pueden ser negativas")
        Integer reentradaHoras,

        // Opcional: depende del producto y del cultivo (ver la etiqueta)
        @Min(value = 0, message = "Los días de carencia no pueden ser negativos")
        @Max(value = 365, message = "Los días de carencia no pueden superar 365")
        Integer diasCarencia) {}
