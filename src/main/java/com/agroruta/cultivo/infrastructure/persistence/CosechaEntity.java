package com.agroruta.cultivo.infrastructure.persistence;

import com.agroruta.cultivo.domain.CalidadCosecha;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "cosechas")
public class CosechaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private Double cantidadKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CalidadCosecha calidad;

    private String observaciones;

    @Column(nullable = false)
    private Long siembraId;

    public CosechaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Double getCantidadKg() { return cantidadKg; }
    public void setCantidadKg(Double cantidadKg) { this.cantidadKg = cantidadKg; }

    public CalidadCosecha getCalidad() { return calidad; }
    public void setCalidad(CalidadCosecha calidad) { this.calidad = calidad; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public Long getSiembraId() { return siembraId; }
    public void setSiembraId(Long siembraId) { this.siembraId = siembraId; }
}