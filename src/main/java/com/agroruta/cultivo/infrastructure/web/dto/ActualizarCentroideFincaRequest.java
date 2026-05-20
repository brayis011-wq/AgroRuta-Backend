package com.agroruta.cultivo.infrastructure.web.dto;

public record ActualizarCentroideFincaRequest(
        Double centroideLat,
        Double centroideLng
) {}