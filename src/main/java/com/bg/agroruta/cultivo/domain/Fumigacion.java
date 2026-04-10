package com.bg.agroruta.cultivo.domain;

import java.time.LocalDate;

public class Fumigacion {

    private Long id;
    private LocalDate fecha;
    private String producto;
    private Double dosis;
    private UnidadMedida unidadMedida;
    private Double areaAplicada;
    private String observaciones;
    private Long siembraId;

    public Fumigacion() {}

    public Fumigacion(Long id, LocalDate fecha, String producto, Double dosis,
                      UnidadMedida unidadMedida, Double areaAplicada,
                      String observaciones, Long siembraId) {
        this.id = id;
        this.fecha = fecha;
        this.producto = producto;
        this.dosis = dosis;
        this.unidadMedida = unidadMedida;
        this.areaAplicada = areaAplicada;
        this.observaciones = observaciones;
        this.siembraId = siembraId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }

    public Double getDosis() { return dosis; }
    public void setDosis(Double dosis) { this.dosis = dosis; }

    public UnidadMedida getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(UnidadMedida unidadMedida) { this.unidadMedida = unidadMedida; }

    public Double getAreaAplicada() { return areaAplicada; }
    public void setAreaAplicada(Double areaAplicada) { this.areaAplicada = areaAplicada; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public Long getSiembraId() { return siembraId; }
    public void setSiembraId(Long siembraId) { this.siembraId = siembraId; }
}