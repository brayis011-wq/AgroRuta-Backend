package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.domain.EstadoCultivo;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "siembras")
public class SiembraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fechaSiembra;

    @Column(nullable = false)
    private Integer cantidadPlantas;

    // Antes era un enum de uchuva; el inicializador del catálogo convierte la columna a texto
    @Column(nullable = false, length = 80)
    private String variedad;

    @Column(name = "tipo_cultivo", length = 80)
    private String tipoCultivo;

    @Column(name = "variedad_id")
    private Long variedadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCultivo estadoCultivo;

    // Ya no es único: un lote guarda todas sus temporadas (solo una activa a la vez)
    @Column(nullable = false)
    private Long loteId;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    public SiembraEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getFechaSiembra() { return fechaSiembra; }
    public void setFechaSiembra(LocalDate fechaSiembra) { this.fechaSiembra = fechaSiembra; }

    public Integer getCantidadPlantas() { return cantidadPlantas; }
    public void setCantidadPlantas(Integer cantidadPlantas) { this.cantidadPlantas = cantidadPlantas; }

    public String getVariedad() { return variedad; }
    public void setVariedad(String variedad) { this.variedad = variedad; }

    public String getTipoCultivo() { return tipoCultivo; }
    public void setTipoCultivo(String tipoCultivo) { this.tipoCultivo = tipoCultivo; }

    public Long getVariedadId() { return variedadId; }
    public void setVariedadId(Long variedadId) { this.variedadId = variedadId; }

    public EstadoCultivo getEstadoCultivo() { return estadoCultivo; }
    public void setEstadoCultivo(EstadoCultivo estadoCultivo) { this.estadoCultivo = estadoCultivo; }

    public Long getLoteId() { return loteId; }
    public void setLoteId(Long loteId) { this.loteId = loteId; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
}