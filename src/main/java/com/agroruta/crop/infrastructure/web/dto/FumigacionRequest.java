package com.agroruta.crop.infrastructure.web.dto;

import com.agroruta.crop.domain.RestriccionesFumigacion;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FumigacionRequest(
        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha de fumigación no puede ser futura")
        LocalDate fecha,

        @NotBlank(message = "El producto es obligatorio")
        @Size(max = 150, message = "El producto no puede superar 150 caracteres")
        String producto,

        // Opcional: solo viene si el producto se eligió del catálogo.
        Long agriculturalInputId,

        @NotNull(message = "La dosis es obligatoria") @Positive(message = "La dosis debe ser mayor a 0")
        Double dosis,

        // El valor exacto (LITROS, ML, GRAMOS, KG) lo valida FumigacionService.
        @NotBlank(message = "La unidad de medida es obligatoria")
        String unidadMedida,

        @NotNull(message = "El área aplicada es obligatoria") @Positive(message = "El área aplicada debe ser mayor a 0")
        Double areaAplicada,

        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        String observaciones,

        // Opcionales: vienen del catálogo o de la etiqueta del producto
        @Min(value = 0, message = "Los días de carencia no pueden ser negativos")
        @Max(value = 365, message = "Los días de carencia no pueden superar 365")
        Integer diasCarencia,

        @Min(value = 0, message = "Las horas de reentrada no pueden ser negativas")
        @Max(value = 720, message = "Las horas de reentrada no pueden superar 720")
        Integer reentradaHoras,

        // Opcional: costo del producto aplicado (COP)
        @PositiveOrZero(message = "El costo no puede ser negativo")
        @DecimalMax(value = "999999999999.99", message = "El valor es demasiado grande")
        BigDecimal costo,

        // Opcional: producto gastado en total (la dosis es una concentración). Si el producto es del
        // catálogo, se descuenta de la bodega de insumos. Unidad: LITROS, ML, GRAMOS o KG.
        @Positive(message = "La cantidad usada debe ser mayor a 0")
        Double cantidadUsada,

        String unidadCantidad,

        @NotNull(message = "La siembra es obligatoria") @Positive(message = "La siembra no es válida")
        Long siembraId) {

    public RestriccionesFumigacion restricciones() {
        return new RestriccionesFumigacion(diasCarencia, reentradaHoras, costo, cantidadUsada, unidadCantidad);
    }
}
