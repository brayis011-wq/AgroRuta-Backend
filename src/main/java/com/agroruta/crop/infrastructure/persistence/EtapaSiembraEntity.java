package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.domain.EstadoCultivo;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "siembra_etapas")
public class EtapaSiembraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "siembra_id", nullable = false)
    private Long siembraId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCultivo etapa;

    @Column(nullable = false)
    private LocalDate fecha;

    public EtapaSiembraEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSiembraId() { return siembraId; }
    public void setSiembraId(Long siembraId) { this.siembraId = siembraId; }

    public EstadoCultivo getEtapa() { return etapa; }
    public void setEtapa(EstadoCultivo etapa) { this.etapa = etapa; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
}
