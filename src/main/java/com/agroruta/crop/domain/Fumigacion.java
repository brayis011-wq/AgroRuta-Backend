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
    /** Días sin cosechar después de aplicar (periodo de carencia) */
    private Integer diasCarencia;
    /** Horas sin entrar al lote después de aplicar */
    private Integer reentradaHoras;
    /** Costo del producto aplicado (COP) */
    private Double costo;
    /** Producto gastado en total (se descuenta de la bodega); la dosis es una concentración */
    private Double cantidadUsada;
    private UnidadMedida unidadCantidad;

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

    public void fijarRestricciones(RestriccionesFumigacion r) {
        if (r == null) return;
        this.diasCarencia = r.diasCarencia();
        this.reentradaHoras = r.reentradaHoras();
        this.costo = r.costo();
        if (r.tieneCantidadUsada()) {
            this.cantidadUsada = r.cantidadUsada();
            this.unidadCantidad = UnidadMedida.valueOf(r.unidadCantidad().trim().toUpperCase());
        } else {
            this.cantidadUsada = null;
            this.unidadCantidad = null;
        }
    }

    /** ¿Hay que descontar producto de la bodega? Solo con insumo del catálogo y cantidad usada */
    public boolean descuentaDeBodega() {
        return tieneInsumoDelCatalogo() && cantidadUsada != null && unidadCantidad != null;
    }

    public boolean tieneInsumoDelCatalogo() {
        return this.agriculturalInputId != null;
    }
}