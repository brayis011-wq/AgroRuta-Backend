package com.agroruta.cultivo.infrastructure.web.dto;

public class LoteRequest {
    private String nombre;
    private Double area;
    private Long fincaId;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }

    public Long getFincaId() { return fincaId; }
    public void setFincaId(Long fincaId) { this.fincaId = fincaId; }
}