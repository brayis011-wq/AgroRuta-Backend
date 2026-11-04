package com.agroruta.crop.infrastructure.web.dto;

import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/**
 * @param fecha             día en que terminó la temporada (vacío = hoy)
 * @param dejarEnDescanso   true: el lote queda en descanso; false: disponible para sembrar
 */
public record FinalizarSiembraRequest(
        @PastOrPresent(message = "La fecha no puede ser futura") LocalDate fecha,
        boolean dejarEnDescanso) {}
