package com.agroruta.report.application.ports.out;

import com.agroruta.report.domain.CostosTemporada;
import java.util.Optional;

public interface CostosQueryPort {

    /** Costos e ingresos de una temporada; vacío si no se pueden calcular (p. ej. la siembra ya no existe). */
    Optional<CostosTemporada> costosDe(Long siembraId);
}
