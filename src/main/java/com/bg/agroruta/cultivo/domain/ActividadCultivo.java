package com.bg.agroruta.cultivo.domain;

import java.time.LocalDate;

public class ActividadCultivo {

    private Long id;
    private TipoActividad tipo;
    private String descripcion;
    private LocalDate fecha;
    private Long siembraId;

    public ActividadCultivo() {}

    public ActividadCultivo(Long id, TipoActividad tipo, String descripcion,
                            LocalDate fecha, Long siembraId) {
        this.id = id;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.siembraId = siembraId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TipoActividad getTipo() { return tipo; }
    public void setTipo(TipoActividad tipo) { this.tipo = tipo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Long getSiembraId() { return siembraId; }
    public void setSiembraId(Long siembraId) { this.siembraId = siembraId; }
}