package com.agroruta.crop.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ActividadRequest(
        // El valor exacto (PODA, TUTOREO, ...) lo valida ActividadCultivoService.
        @NotBlank(message = "El tipo es obligatorio") String tipo,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String descripcion,

        // Sin @PastOrPresent: una actividad puede quedar programada.
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,

        // Opcional: materiales o servicios de la actividad (COP), aparte de los jornales
        @PositiveOrZero(message = "El costo no puede ser negativo")
        @DecimalMax(value = "999999999999.99", message = "El valor es demasiado grande")
        BigDecimal costo,

        // Opcional: el insumo que se gastó (abonado...). De la bodega y con cantidad, se descuenta de ella
        @Size(max = 150, message = "El producto no puede superar 150 caracteres")
        String producto,

        @Positive(message = "El insumo no es válido") Long agriculturalInputId,

        @Positive(message = "La cantidad usada debe ser mayor a 0")
        @DecimalMax(value = "99999999", message = "La cantidad es demasiado grande")
        Double cantidadUsada,

        /** LITROS, ML, KG o GRAMOS (lo valida el servicio) */
        @Size(max = 10, message = "La unidad no es válida") String unidadCantidad,

        @NotNull(message = "La siembra es obligatoria") @Positive(message = "La siembra no es válida")
        Long siembraId) {

    public com.agroruta.crop.domain.UsoInsumo insumo() {
        boolean hay = (producto != null && !producto.isBlank()) || agriculturalInputId != null;
        return hay
                ? new com.agroruta.crop.domain.UsoInsumo(agriculturalInputId, producto, cantidadUsada, unidadCantidad)
                : null;
    }
}
