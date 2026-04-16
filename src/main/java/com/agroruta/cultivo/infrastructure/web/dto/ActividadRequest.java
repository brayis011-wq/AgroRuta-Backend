package com.agroruta.cultivo.infrastructure.web.dto;

import java.time.LocalDate;

public class ActividadRequest {
    private String tipo;
    private String descripcion;
    private LocalDate fecha;
    private Long siembraId;

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Long getSiembraId() { return siembraId; }
    public void setSiembraId(Long siembraId) { this.siembraId = siembraId; }
}