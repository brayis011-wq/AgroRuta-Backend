package com.agroruta.crop.infrastructure.web.dto;

public record ActualizarCentroideFincaRequest(
        Double centroideLat,
        Double centroideLng
) {}