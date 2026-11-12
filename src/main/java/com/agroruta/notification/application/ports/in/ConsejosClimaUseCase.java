package com.agroruta.notification.application.ports.in;

import com.agroruta.weather.domain.ConsejoClima;

import java.util.List;

/** Cruza el pronóstico de cada finca con lo que el agricultor tiene programado hoy y mañana. */
public interface ConsejosClimaUseCase {

    /** Lo más urgente primero. Vacío si no hay nada que avisar o no hay pronóstico. */
    List<ConsejoClima> consejos(Long agricultorId);
}
