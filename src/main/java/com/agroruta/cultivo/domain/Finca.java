package com.agroruta.cultivo.domain;

import java.time.LocalDateTime;

public class Finca {

    private Long id;
    private String nombre;
    private String ubicacion;
    private Double hectareas;
    private Long agricultorId;
    private LocalDateTime fechaRegistro;

    public Finca() {}

    public Finca(Long id, String nombre, String ubicacion, Double hectareas, Long agricultorId) {
        this.id = id;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.hectareas = hectareas;
        this.agricultorId = agricultorId;
        this.fechaRegistro = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public Double getHectareas() { return hectareas; }
    public void setHectareas(Double hectareas) { this.hectareas = hectareas; }

    public Long getAgricultorId() { return agricultorId; }
    public void setAgricultorId(Long agricultorId) { this.agricultorId = agricultorId; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}