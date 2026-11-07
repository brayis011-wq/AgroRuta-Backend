package com.agroruta.report.domain;

import java.time.LocalDate;

public record CropDetail(
        Long siembraId,
        String variedad,           // "Cultivo · Variedad", p. ej. "Café · Castillo"
        String estado,             // EstadoCultivo enum como string
        LocalDate fechaSiembra,    // fecha de siembra, no hay fecha de inicio de etapa
        long diasDesdeSiembra,     // días desde que se sembró
        Double totalKgCosechado,
        int totalCosechas,
        String cultivo,            // solo el cultivo, p. ej. "Café" (null en siembras antiguas)
        String loteNombre,
        String fincaNombre,
        // La siembra, su lote o su finca se eliminaron (borrado lógico): ya no se ve en las
        // pantallas, pero se conserva en los reportes con su historial de gastos e ingresos
        boolean eliminada
) {
    public CropDetail(Long siembraId, String variedad, String estado, LocalDate fechaSiembra,
                      long diasDesdeSiembra, Double totalKgCosechado, int totalCosechas,
                      String cultivo, String loteNombre, String fincaNombre) {
        this(siembraId, variedad, estado, fechaSiembra, diasDesdeSiembra, totalKgCosechado, totalCosechas,
                cultivo, loteNombre, fincaNombre, false);
    }

    public CropDetail(Long siembraId, String variedad, String estado, LocalDate fechaSiembra,
                      long diasDesdeSiembra, Double totalKgCosechado, int totalCosechas) {
        this(siembraId, variedad, estado, fechaSiembra, diasDesdeSiembra, totalKgCosechado, totalCosechas,
                null, null, null, false);
    }
}
