package com.agroruta.cultivo.infrastructure.persistence;

import com.agroruta.cultivo.domain.EstadoLote;
import jakarta.persistence.*;

@Entity
@Table(name = "lotes", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_lote_nombre_finca",
                columnNames = {"nombre", "fincaId"}
        )
})
public class LoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private Double area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoLote estado;

    @Column(nullable = false)
    private Long fincaId;

    @Column(columnDefinition = "TEXT")
    private String coordenadas;

    private Double centroideLat;
    private Double centroideLng;

    public LoteEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }

    public EstadoLote getEstado() { return estado; }
    public void setEstado(EstadoLote estado) { this.estado = estado; }

    public Long getFincaId() { return fincaId; }
    public void setFincaId(Long fincaId) { this.fincaId = fincaId; }

    public String getCoordenadas() { return coordenadas; }
    public void setCoordenadas(String coordenadas) { this.coordenadas = coordenadas; }

    public Double getCentroideLat() { return centroideLat; }
    public void setCentroideLat(Double centroideLat) { this.centroideLat = centroideLat; }

    public Double getCentroideLng() { return centroideLng; }
    public void setCentroideLng(Double centroideLng) { this.centroideLng = centroideLng; }
}