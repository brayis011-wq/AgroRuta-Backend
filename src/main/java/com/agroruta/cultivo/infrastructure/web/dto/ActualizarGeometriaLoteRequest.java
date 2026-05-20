package com.agroruta.cultivo.infrastructure.web.dto;

public record ActualizarGeometriaLoteRequest(
    String coordenadas,   // GeoJSON del polígono
    Double area,          // calculada en el frontend (ha)
    Double centroideLat,
    Double centroideLng
) {}