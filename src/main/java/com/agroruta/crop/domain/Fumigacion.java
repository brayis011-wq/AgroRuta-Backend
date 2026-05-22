package com.agroruta.crop.domain;

import java.time.LocalDate;

/**
 * Modelo de dominio de Fumigación (refactorizado).
 *
 * Cambios respecto a la versión original:
 * ─────────────────────────────────────────────────────────────────────────────
 * 1. agriculturalInputId (Long, nullable):
 *    Referencia al catálogo de insumos del módulo agricultural-input.
 *    Si el usuario seleccionó un insumo del catálogo, se guarda el ID.
 *    Si lo escribió manualmente, este campo queda null y `producto` contiene
 *    el nombre libre. Nunca depende del objeto AgriculturalInput completo
 *    (no rompemos la separación de bounded contexts).
 *
 * 2. Se añade factory method estático para entrada válida (datos siempre consistentes).
 *
 * 3. Se mantiene retrocompatibilidad: producto sigue siendo String para
 *    registros históricos y entradas manuales.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class Fumigacion {

    private Long id;
    private LocalDate fecha;

    // Nombre del producto (libre o copiado del catálogo al momento del registro)
    private String producto;

    // Referencia opcional al catálogo de insumos (null si fue entrada manual)
    private Long agriculturalInputId;

    private Double dosis;
    private UnidadMedida unidadMedida;
    private Double areaAplicada;
    private String observaciones;
    private Long siembraId;

    public Fumigacion() {}

    // ── Factory method principal ───────────────────────────────────────────────

    /**
     * Crea una fumigación desde el catálogo.
     * El agriculturalInputId establece el vínculo con el insumo seleccionado.
     */
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

    /**
     * Crea una fumigación con producto escrito manualmente.
     * agriculturalInputId queda null.
     */
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

    /** Indica si este registro está vinculado a un insumo del catálogo. */
    public boolean tieneInsumoDelCatalogo() {
        return this.agriculturalInputId != null;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public LocalDate getFecha()                { return fecha; }
    public void setFecha(LocalDate fecha)      { this.fecha = fecha; }

    public String getProducto()                { return producto; }
    public void setProducto(String producto)   { this.producto = producto; }

    public Long getAgriculturalInputId()       { return agriculturalInputId; }
    public void setAgriculturalInputId(Long a) { this.agriculturalInputId = a; }

    public Double getDosis()                   { return dosis; }
    public void setDosis(Double dosis)         { this.dosis = dosis; }

    public UnidadMedida getUnidadMedida()      { return unidadMedida; }
    public void setUnidadMedida(UnidadMedida u){ this.unidadMedida = u; }

    public Double getAreaAplicada()            { return areaAplicada; }
    public void setAreaAplicada(Double a)      { this.areaAplicada = a; }

    public String getObservaciones()           { return observaciones; }
    public void setObservaciones(String o)     { this.observaciones = o; }

    public Long getSiembraId()                 { return siembraId; }
    public void setSiembraId(Long siembraId)   { this.siembraId = siembraId; }
}