package com.agroruta.worker.domain;

import java.util.List;

/**
 * Lo que el Inicio necesita saber de los jornales, ya contado por la base: cuántos van este mes,
 * cuántos hubo el mes anterior y qué trabajadores ya tienen jornal hoy.
 * Antes la pantalla pedía los jornales de cada trabajador activo (una consulta por trabajador).
 */
public record ResumenJornalesInicio(long mesActual, long mesAnterior, List<Long> trabajadoresHoy) {

    public ResumenJornalesInicio {
        trabajadoresHoy = trabajadoresHoy == null ? List.of() : List.copyOf(trabajadoresHoy);
    }
}
