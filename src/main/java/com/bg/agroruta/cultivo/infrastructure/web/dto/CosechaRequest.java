package com.bg.agroruta.cultivo.infrastructure.web.dto;

import java.time.LocalDate;

public class CosechaRequest {
    private LocalDate fecha;
    private Double cantidadKg;
    private String calidad;
    private String observaciones;
    private Long siembraId;

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Double getCantidadKg() { return cantidadKg; }
    public void setCantidadKg(Double cantidadKg) { this.cantidadKg = cantidadKg; }

    public String getCalidad() { return calidad; }
    public void setCalidad(String calidad) { this.calidad = calidad; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public Long getSiembraId() { return siembraId; }
    public void setSiembraId(Long siembraId) { this.siembraId = siembraId; }
}