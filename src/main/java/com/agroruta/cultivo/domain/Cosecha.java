package com.agroruta.cultivo.domain;

import java.time.LocalDate;

public class Cosecha {

    private Long id;
    private LocalDate fecha;
    private Double cantidadKg;
    private CalidadCosecha calidad;
    private String observaciones;
    private Long siembraId;

    public Cosecha() {}

    public Cosecha(Long id, LocalDate fecha, Double cantidadKg,
                   CalidadCosecha calidad, String observaciones, Long siembraId) {
        this.id = id;
        this.fecha = fecha;
        this.cantidadKg = cantidadKg;
        this.calidad = calidad;
        this.observaciones = observaciones;
        this.siembraId = siembraId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Double getCantidadKg() { return cantidadKg; }
    public void setCantidadKg(Double cantidadKg) { this.cantidadKg = cantidadKg; }

    public CalidadCosecha getCalidad() { return calidad; }
    public void setCalidad(CalidadCosecha calidad) { this.calidad = calidad; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public Long getSiembraId() { return siembraId; }
    public void setSiembraId(Long siembraId) { this.siembraId = siembraId; }
}