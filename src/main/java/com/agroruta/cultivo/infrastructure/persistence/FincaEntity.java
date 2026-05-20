package com.agroruta.cultivo.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fincas", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_finca_nombre_agricultor",
                columnNames = {"nombre", "agricultorId"}
        )
})
public class FincaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String ubicacion;

    @Column(nullable = false)
    private Double hectareas;

    @Column(nullable = false)
    private Long agricultorId;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    private Double centroideLat;
    private Double centroideLng;

    public FincaEntity() {}

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

    public Double getCentroideLat() { return centroideLat; }
    public void setCentroideLat(Double centroideLat) { this.centroideLat = centroideLat; }

    public Double getCentroideLng() { return centroideLng; }
    public void setCentroideLng(Double centroideLng) { this.centroideLng = centroideLng; }
}