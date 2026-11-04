package com.agroruta.portal.infrastructure.web.dto;

import com.agroruta.worker.domain.Nomina;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MiNominaResponse(Long id, LocalDate periodoInicio, LocalDate periodoFin,
                               int totalJornales, BigDecimal valorTotal, String estado) {

    public static MiNominaResponse from(Nomina n) {
        return new MiNominaResponse(n.getId(), n.getPeriodoInicio(), n.getPeriodoFin(),
                n.getTotalJornales(), n.getValorTotal(), n.getEstado() != null ? n.getEstado().name() : null);
    }
}
