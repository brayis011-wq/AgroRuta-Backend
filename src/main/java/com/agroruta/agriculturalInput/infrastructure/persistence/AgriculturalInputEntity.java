package com.agroruta.agriculturalInput.infrastructure.persistence;


import com.agroruta.agriculturalInput.domain.AgriculturalInputType;
import com.agroruta.agriculturalInput.domain.MeasurementUnit;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA. Pertenece exclusivamente a la capa de infraestructura.
 * El dominio nunca conoce esta clase.
 */
@Entity
@Table(name = "agricultural_inputs")
public class AgriculturalInputEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AgriculturalInputType tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_sugerida", nullable = false, length = 10)
    private MeasurementUnit unidadSugerida;

    @Column(name = "dosis_sugerida", nullable = false)
    private Double dosisSugerida;

    @Column(name = "reentrada_horas", nullable = false)
    private Integer reentradaHoras;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getNombre()                  { return nombre; }
    public void setNombre(String nombre)       { this.nombre = nombre; }

    public AgriculturalInputType getTipo()     { return tipo; }
    public void setTipo(AgriculturalInputType tipo) { this.tipo = tipo; }

    public MeasurementUnit getUnidadSugerida() { return unidadSugerida; }
    public void setUnidadSugerida(MeasurementUnit u) { this.unidadSugerida = u; }

    public Double getDosisSugerida()           { return dosisSugerida; }
    public void setDosisSugerida(Double d)     { this.dosisSugerida = d; }

    public Integer getReentradaHoras()         { return reentradaHoras; }
    public void setReentradaHoras(Integer r)   { this.reentradaHoras = r; }

    public Boolean isActivo()                  { return activo; }
    public void setActivo(Boolean activo)      { this.activo = activo; }

    public LocalDateTime getCreadoEn()         { return creadoEn; }
    public void setCreadoEn(LocalDateTime t)   { this.creadoEn = t; }
}