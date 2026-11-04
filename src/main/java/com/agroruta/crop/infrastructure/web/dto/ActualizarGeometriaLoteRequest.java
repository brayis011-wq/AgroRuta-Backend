package com.agroruta.crop.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ActualizarGeometriaLoteRequest(
        @NotBlank(message = "Las coordenadas son obligatorias")
        @Size(max = 100_000, message = "La geometría es demasiado grande")
        String coordenadas,   // GeoJSON del polígono

        @Positive(message = "El área debe ser mayor a 0")
        Double area,          // calculada en el frontend (ha)

        @DecimalMin(value = "-90", message = "Latitud fuera de rango")
        @DecimalMax(value = "90", message = "Latitud fuera de rango")
        Double centroideLat,

        @DecimalMin(value = "-180", message = "Longitud fuera de rango")
        @DecimalMax(value = "180", message = "Longitud fuera de rango")
        Double centroideLng
) {}
