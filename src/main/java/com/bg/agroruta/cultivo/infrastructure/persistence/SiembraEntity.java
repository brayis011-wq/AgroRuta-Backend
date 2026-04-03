package com.bg.agroruta.cultivo.infrastructure.persistence;

import com.bg.agroruta.cultivo.domain.EstadoCultivo;
import com.bg.agroruta.cultivo.domain.VariedadUchuva;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VariedadUchuva variedad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCultivo estadoCultivo;

    @Column(nullable = false, unique = true)
    private Long loteId;

    public SiembraEntity() {}

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