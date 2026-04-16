package com.agroruta.cultivo.domain;

import java.time.LocalDate;

public class Siembra {

    private Long id;
    private LocalDate fechaSiembra;
    private Integer cantidadPlantas;
    private VariedadUchuva variedad;
    private EstadoCultivo estadoCultivo;
    private Long loteId;

    public Siembra() {}

    public Siembra(Long id, LocalDate fechaSiembra, Integer cantidadPlantas,
                   VariedadUchuva variedad, Long loteId) {
        this.id = id;
        this.fechaSiembra = fechaSiembra;
        this.cantidadPlantas = cantidadPlantas;
        this.variedad = variedad;
        this.loteId = loteId;
        this.estadoCultivo = EstadoCultivo.GERMINACION;
    }

    // Lógica de negocio — avanzar etapa del cultivo
    public void avanzarEtapa() {
        switch (this.estadoCultivo) {
            case GERMINACION -> this.estadoCultivo = EstadoCultivo.CRECIMIENTO;
            case CRECIMIENTO -> this.estadoCultivo = EstadoCultivo.PRODUCCION;
            case PRODUCCION -> this.estadoCultivo = EstadoCultivo.COSECHA;
            case COSECHA -> this.estadoCultivo = EstadoCultivo.FINALIZADO;
            case FINALIZADO -> throw new IllegalStateException("El cultivo ya está finalizado.");
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getFechaSiembra() { return fechaSiembra; }
    public void setFechaSiembra(LocalDate fechaSiembra) { this.fechaSiembra = fechaSiembra; }

    public Integer getCantidadPlantas() { return cantidadPlantas; }
    public void setCantidadPlantas(Integer cantidadPlantas) { this.cantidadPlantas = cantidadPlantas; }

    public VariedadUchuva getVariedad() { return variedad; }
    public void setVariedad(VariedadUchuva variedad) { this.variedad = variedad; }

    public EstadoCultivo getEstadoCultivo() { return estadoCultivo; }
    public void setEstadoCultivo(EstadoCultivo estadoCultivo) { this.estadoCultivo = estadoCultivo; }

    public Long getLoteId() { return loteId; }
    public void setLoteId(Long loteId) { this.loteId = loteId; }
}