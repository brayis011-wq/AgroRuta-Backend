package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.domain.EstadoNomina;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "nominas")
public class NominaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trabajador_id", nullable = false)
    private TrabajadorEntity trabajador;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;

    @Column(name = "periodo_fin", nullable = false)
    private LocalDate periodoFin;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "nomina_jornales",
            joinColumns = @JoinColumn(name = "nomina_id"),
            inverseJoinColumns = @JoinColumn(name = "jornal_id")
    )
    private List<JornalEntity> jornales = new ArrayList<>();

    @Column(name = "total_jornales", nullable = false)
    private int totalJornales;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoNomina estado;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDate fechaGeneracion;

    @Column(length = 500)
    private String observaciones;

    public NominaEntity() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TrabajadorEntity getTrabajador() {
        return trabajador;
    }

    public void setTrabajador(TrabajadorEntity trabajador) {
        this.trabajador = trabajador;
    }

    public LocalDate getPeriodoInicio() {
        return periodoInicio;
    }

    public void setPeriodoInicio(LocalDate periodoInicio) {
        this.periodoInicio = periodoInicio;
    }

    public LocalDate getPeriodoFin() {
        return periodoFin;
    }

    public void setPeriodoFin(LocalDate periodoFin) {
        this.periodoFin = periodoFin;
    }

    public List<JornalEntity> getJornales() {
        return jornales;
    }

    public void setJornales(List<JornalEntity> jornales) {
        this.jornales = jornales;
    }

    public int getTotalJornales() {
        return totalJornales;
    }

    public void setTotalJornales(int totalJornales) {
        this.totalJornales = totalJornales;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public EstadoNomina getEstado() {
        return estado;
    }

    public void setEstado(EstadoNomina estado) {
        this.estado = estado;
    }

    public LocalDate getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDate fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}