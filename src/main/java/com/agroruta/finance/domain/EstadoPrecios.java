package com.agroruta.finance.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cómo van los precios de SIPSA: si ya hay, de qué semana, si se están trayendo ahora y, si la última
 * vez falló, por qué. Con esto la pantalla no dice "no está" cuando todavía no sabe.
 */
public record EstadoPrecios(
        boolean hayPrecios,
        LocalDate ultimaSemana,
        LocalDateTime actualizadoEn,
        boolean actualizando,
        LocalDateTime ultimoIntento,
        String ultimoError) {}
