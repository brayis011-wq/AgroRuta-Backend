package com.agroruta.agriculturalInput.domain;

import java.time.LocalDateTime;


public class AgriculturalInput {

    private Long id;
    private String nombre;
    private AgriculturalInputType tipo;
    private MeasurementUnit unidadSugerida;
    private Double dosisSugerida;
    private Integer reentradaHoras;
    private Boolean activo;
    private LocalDateTime creadoEn;

    // Constructor privado: se instancia solo mediante factory methods o reconstitución desde persistencia
    private AgriculturalInput() {}

    // ── Factory method ────────────────────────────────────────────────────────

    public static AgriculturalInput create(
            String nombre,
            AgriculturalInputType tipo,
            MeasurementUnit unidadSugerida,
            Double dosisSugerida,
            Integer reentradaHoras) {

        AgriculturalInput input = new AgriculturalInput();
        input.nombre        = nombre;
        input.tipo          = tipo;
        input.unidadSugerida = unidadSugerida;
        input.dosisSugerida = dosisSugerida;
        input.reentradaHoras = reentradaHoras;
        input.activo        = true;
        input.creadoEn      = LocalDateTime.now();
        return input;
    }

    // ── Comportamiento de dominio ─────────────────────────────────────────────

    public void update(
            String nombre,
            AgriculturalInputType tipo,
            MeasurementUnit unidadSugerida,
            Double dosisSugerida,
            Integer reentradaHoras) {

        this.nombre          = nombre;
        this.tipo            = tipo;
        this.unidadSugerida  = unidadSugerida;
        this.dosisSugerida   = dosisSugerida;
        this.reentradaHoras  = reentradaHoras;
    }

    /** Soft delete: desactiva el insumo sin borrarlo de la base de datos. */
    public void deactivate() {
        this.activo = false;
    }

    public boolean requiresReentryPeriod() {
        return this.reentradaHoras != null && this.reentradaHoras > 0;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId()                      { return id; }
    public String getNombre()                { return nombre; }
    public AgriculturalInputType getTipo()   { return tipo; }
    public MeasurementUnit getUnidadSugerida() { return unidadSugerida; }
    public Double getDosisSugerida()         { return dosisSugerida; }
    public Integer getReentradaHoras()       { return reentradaHoras; }
    public Boolean isActivo()                { return activo; }
    public LocalDateTime getCreadoEn()       { return creadoEn; }

    // ── Setters solo para reconstitución desde persistencia ──────────────────

    public void setId(Long id)               { this.id = id; }
    public void setActivo(Boolean activo)    { this.activo = activo; }
    public void setCreadoEn(LocalDateTime t) { this.creadoEn = t; }
}