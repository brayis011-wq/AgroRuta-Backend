package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.domain.TipoActividad;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "actividades_cultivo",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_actividad_tipo_fecha_siembra",
                    columnNames = {"tipo", "fecha", "siembraId"})
        })
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

    @Column(precision = 14, scale = 2)
    private BigDecimal costo;

    @Column(length = 150)
    private String producto;

    @Column(name = "agricultural_input_id")
    private Long agriculturalInputId;

    @Column(name = "cantidad_usada")
    private Double cantidadUsada;

    @Column(name = "unidad_cantidad", length = 10)
    private String unidadCantidad;

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }

    public Long getAgriculturalInputId() {
        return agriculturalInputId;
    }

    public void setAgriculturalInputId(Long agriculturalInputId) {
        this.agriculturalInputId = agriculturalInputId;
    }

    public Double getCantidadUsada() {
        return cantidadUsada;
    }

    public void setCantidadUsada(Double cantidadUsada) {
        this.cantidadUsada = cantidadUsada;
    }

    public String getUnidadCantidad() {
        return unidadCantidad;
    }

    public void setUnidadCantidad(String unidadCantidad) {
        this.unidadCantidad = unidadCantidad;
    }

    public ActividadCultivoEntity() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoActividad getTipo() {
        return tipo;
    }

    public void setTipo(TipoActividad tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Long getSiembraId() {
        return siembraId;
    }

    public void setSiembraId(Long siembraId) {
        this.siembraId = siembraId;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }
}
