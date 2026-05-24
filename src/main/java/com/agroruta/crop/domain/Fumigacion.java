package com.agroruta.crop.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

/**
 * Modelo de dominio de Fumigación (refactorizado).
 * ...
 */
@Getter
@Setter
@NoArgsConstructor
public class Fumigacion {

    private Long id;
    private LocalDate fecha;
    private String producto;
    private Long agriculturalInputId;
    private Double dosis;
    private UnidadMedida unidadMedida;
    private Double areaAplicada;
    private String observaciones;
    private Long siembraId;

    public static Fumigacion fromCatalog(
            LocalDate fecha,
            String producto,
            Long agriculturalInputId,
            Double dosis,
            UnidadMedida unidadMedida,
            Double areaAplicada,
            String observaciones,
            Long siembraId) {

        Fumigacion f = new Fumigacion();
        f.fecha               = fecha;
        f.producto            = producto;
        f.agriculturalInputId = agriculturalInputId;
        f.dosis               = dosis;
        f.unidadMedida        = unidadMedida;
        f.areaAplicada        = areaAplicada;
        f.observaciones       = observaciones;
        f.siembraId           = siembraId;
        return f;
    }

    public static Fumigacion fromManualEntry(
            LocalDate fecha,
            String producto,
            Double dosis,
            UnidadMedida unidadMedida,
            Double areaAplicada,
            String observaciones,
            Long siembraId) {

        return fromCatalog(fecha, producto, null, dosis, unidadMedida,
                areaAplicada, observaciones, siembraId);
    }

    public boolean tieneInsumoDelCatalogo() {
        return this.agriculturalInputId != null;
    }
}