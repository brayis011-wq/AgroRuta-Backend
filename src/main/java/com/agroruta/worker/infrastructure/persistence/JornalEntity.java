package com.agroruta.worker.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jornales")
public class JornalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trabajador_id", nullable = false)
    private TrabajadorEntity trabajador;

    @Column(name = "cultivo_id", nullable = false)
    private Long cultivoId;

    @Column(name = "nombre_cultivo", length = 150)
    private String nombreCultivo;

    /**
     * Relación ManyToMany: un jornal puede tener múltiples actividades
     * y una actividad puede aparecer en múltiples jornales.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "jornal_actividades",
        joinColumns = @JoinColumn(name = "jornal_id"),
        inverseJoinColumns = @JoinColumn(name = "actividad_id")
    )
    private List<ActividadEntity> actividades = new ArrayList<>();

    @Column(length = 500)
    private String observaciones;

    @Column(name = "valor_jornal", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorJornal;

    @Column(nullable = false)
    private boolean liquidado;

    public JornalEntity() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public TrabajadorEntity getTrabajador() { return trabajador; }
    public void setTrabajador(TrabajadorEntity trabajador) { this.trabajador = trabajador; }

    public Long getCultivoId() { return cultivoId; }
    public void setCultivoId(Long cultivoId) { this.cultivoId = cultivoId; }

    public String getNombreCultivo() { return nombreCultivo; }
    public void setNombreCultivo(String nombreCultivo) { this.nombreCultivo = nombreCultivo; }

    public List<ActividadEntity> getActividades() { return actividades; }
    public void setActividades(List<ActividadEntity> actividades) { this.actividades = actividades; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public BigDecimal getValorJornal() { return valorJornal; }
    public void setValorJornal(BigDecimal valorJornal) { this.valorJornal = valorJornal; }

    public boolean isLiquidado() { return liquidado; }
    public void setLiquidado(boolean liquidado) { this.liquidado = liquidado; }
}
