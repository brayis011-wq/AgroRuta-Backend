package com.agroruta.portal.infrastructure.web.dto;

import com.agroruta.worker.domain.Actividad;
import com.agroruta.worker.domain.Jornal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MiJornalResponse(Long id, LocalDate fecha, String cultivo, List<String> actividades,
                               BigDecimal valor, boolean liquidado) {

    public static MiJornalResponse from(Jornal j) {
        List<String> actividades = j.getActividades() == null ? List.of()
                : j.getActividades().stream().map(Actividad::getNombre).toList();
        return new MiJornalResponse(j.getId(), j.getFecha(), j.getNombreCultivo(), actividades,
                j.getValorJornal(), j.isLiquidado());
    }
}
