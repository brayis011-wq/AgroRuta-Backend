package com.agroruta.report.infrastructure.adapter;

import com.agroruta.crop.application.CostosCalendarioService;
import com.agroruta.crop.domain.CostosSiembra;
import com.agroruta.report.application.ports.out.CostosQueryPort;
import com.agroruta.report.domain.CostosTemporada;
import com.agroruta.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Toma los costos del módulo de cultivos: los mismos números que se ven en la pantalla de costos. */
@Component
@RequiredArgsConstructor
public class CostosQueryAdapter implements CostosQueryPort {

    private final CostosCalendarioService costosService;

    @Override
    public Optional<CostosTemporada> costosDe(Long siembraId) {
        try {
            CostosSiembra c = costosService.costos(siembraId);
            return Optional.of(new CostosTemporada(c.manoObra(), c.jornales(), c.insumos(), c.otros(),
                    c.costoTotal(), c.ingresos(), c.kgTotal(), c.kgConPrecio(), c.ganancia(), c.costoPorKg()));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }
}
