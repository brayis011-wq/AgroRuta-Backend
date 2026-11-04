package com.agroruta.crop.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "variedades_cultivo",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tipo_cultivo_id", "nombre"}))
public class VariedadCultivoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tipo_cultivo_id", nullable = false)
    private TipoCultivoEntity tipoCultivo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public TipoCultivoEntity getTipoCultivo() { return tipoCultivo; }
    public void setTipoCultivo(TipoCultivoEntity tipoCultivo) { this.tipoCultivo = tipoCultivo; }
}
