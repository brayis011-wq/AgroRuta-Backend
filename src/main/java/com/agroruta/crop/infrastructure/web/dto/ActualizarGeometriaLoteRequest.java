package com.agroruta.crop.infrastructure.web.dto;

public record ActualizarGeometriaLoteRequest(
    String coordenadas,   // GeoJSON del polígono
    Double area,          // calculada en el frontend (ha)
    Double centroideLat,
    Double centroideLng
) {}