package com.agroruta.crop.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.math.BigDecimal;

public record CosechaRequest(
        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha de cosecha no puede ser futura")
        LocalDate fecha,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a 0")
        Double cantidadKg,

        // El valor exacto (PRIMERA, SEGUNDA, TERCERA) lo valida CosechaService.
        @NotBlank(message = "La calidad es obligatoria")
        String calidad,

        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        String observaciones,

        // Opcional: precio de venta por kilo (COP)
        @PositiveOrZero(message = "El precio no puede ser negativo")
        @DecimalMax(value = "999999999999.99", message = "El valor es demasiado grande")
        BigDecimal precioKg,

        // true = la persona confirmó que cosecha aunque no haya terminado la carencia de una fumigación
        Boolean confirmarCarencia,

        /** Quién la recogió y cómo se le paga; null = no se tocan (al editar) */
        @jakarta.validation.Valid
        @Size(max = 60, message = "Son demasiados recolectores para una cosecha")
        java.util.List<RecolectorRequest> recolectores,

        @NotNull(message = "La siembra es obligatoria")
        @Positive(message = "La siembra no es válida")
        Long siembraId
) {

    // Si no viene en el JSON es false (un record con boolean primitivo rechazaría el pedido)
    public CosechaRequest {
        confirmarCarencia = Boolean.TRUE.equals(confirmarCarencia);
    }

    public record RecolectorRequest(
            @NotNull(message = "Elige quién recogió")
            @Positive(message = "El trabajador no es válido")
            Long trabajadorId,

            /** KILO o DIA */
            @NotBlank(message = "Indica si se paga por kilo o por día")
            @jakarta.validation.constraints.Pattern(regexp = "KILO|DIA", message = "La forma de pago debe ser KILO o DIA")
            String modalidad,

            @Positive(message = "Los kilos deben ser mayores a 0")
            @DecimalMax(value = "99999999.99", message = "Son demasiados kilos")
            BigDecimal kilos,

            /** null = el del cargo */
            @Positive(message = "El valor del kilo debe ser mayor a 0")
            @DecimalMax(value = "99999999.99", message = "El valor es demasiado grande")
            BigDecimal valorKilo,

            /** null = el del cargo */
            @Positive(message = "El valor del día debe ser mayor a 0")
            @DecimalMax(value = "99999999.99", message = "El valor es demasiado grande")
            BigDecimal valorDia
    ) {

        public com.agroruta.crop.domain.Recolector aDominio() {
            return new com.agroruta.crop.domain.Recolector(trabajadorId, modalidad, kilos, valorKilo, valorDia);
        }
    }

    public java.util.List<com.agroruta.crop.domain.Recolector> recolectoresDominio() {
        return recolectores == null ? null : recolectores.stream().map(RecolectorRequest::aDominio).toList();
    }
}
