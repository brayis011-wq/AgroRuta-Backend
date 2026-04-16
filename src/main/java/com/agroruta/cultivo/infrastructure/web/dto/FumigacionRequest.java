package com.agroruta.cultivo.infrastructure.web.dto;

import java.time.LocalDate;

public class FumigacionRequest {
    private LocalDate fecha;
    private String producto;
    private Double dosis;
    private String unidadMedida;
    private Double areaAplicada;
    private String observaciones;
    private Long siembraId;

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }

    public Double getDosis() { return dosis; }
    public void setDosis(Double dosis) { this.dosis = dosis; }

    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }

    public Double getAreaAplicada() { return areaAplicada; }
    public void setAreaAplicada(Double areaAplicada) { this.areaAplicada = areaAplicada; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public Long getSiembraId() { return siembraId; }
    public void setSiembraId(Long siembraId) { this.siembraId = siembraId; }
}