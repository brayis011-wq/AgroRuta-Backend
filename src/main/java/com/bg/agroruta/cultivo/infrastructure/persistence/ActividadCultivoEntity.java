package com.bg.agroruta.cultivo.infrastructure.persistence;

import com.bg.agroruta.cultivo.domain.TipoActividad;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "actividades_cultivo")
public class ActividadCultivoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoActividad tipo;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private Long siembraId;

    public ActividadCultivoEntity() {}

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