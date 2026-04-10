package com.bg.agroruta.cultivo.infrastructure.persistence;

import com.bg.agroruta.cultivo.domain.UnidadMedida;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "fumigaciones")
public class FumigacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private String producto;

    @Column(nullable = false)
    private Double dosis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnidadMedida unidadMedida;

    @Column(nullable = false)
    private Double areaAplicada;

    private String observaciones;

    @Column(nullable = false)
    private Long siembraId;

    public FumigacionEntity() {}

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