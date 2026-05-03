package com.agroruta.worker.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Nomina {

    private Long id;
    private Trabajador trabajador;
    private LocalDate periodoInicio;
    private LocalDate periodoFin;
    private List<Jornal> jornales;
    private int totalJornales;
    private BigDecimal valorTotal;
    private EstadoNomina estado;
    private LocalDate fechaGeneracion;
    private String observaciones;

    public Nomina() {
        this.jornales = new ArrayList<>();
        this.estado = EstadoNomina.PENDIENTE;
    }

    public Nomina(Long id, Trabajador trabajador, LocalDate periodoInicio,
                  LocalDate periodoFin, List<Jornal> jornales) {
        this.id = id;
        this.trabajador = trabajador;
        this.periodoInicio = periodoInicio;
        this.periodoFin = periodoFin;
        this.jornales = jornales;
        this.estado = EstadoNomina.PENDIENTE;
        this.fechaGeneracion = LocalDate.now();
        calcular();
    }

    // ── Lógica de dominio ────────────────────────────────────────────────────

    public void calcular() {
        this.totalJornales = this.jornales.size();
        this.valorTotal = this.jornales.stream()
                .map(Jornal::getValorJornal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void aprobar() {
        if (!EstadoNomina.PENDIENTE.equals(this.estado)) {
            throw new IllegalStateException("Solo se pueden aprobar nóminas en estado PENDIENTE.");
        }
        this.estado = EstadoNomina.APROBADA;
    }

    public void anular() {
        if (EstadoNomina.PAGADA.equals(this.estado)) {
            throw new IllegalStateException("No se puede anular una nómina ya pagada.");
        }
        this.estado = EstadoNomina.ANULADA;
    }

    // ✅ NUEVO
    public void reactivar() {
        if (!EstadoNomina.ANULADA.equals(this.estado)) {
            throw new IllegalStateException("Solo se pueden reactivar nóminas en estado ANULADA.");
        }
        this.estado = EstadoNomina.PENDIENTE;
    }

    public void marcarComoPagada() {
        if (!EstadoNomina.APROBADA.equals(this.estado)) {
            throw new IllegalStateException("Solo se pueden pagar nóminas en estado APROBADA.");
        }
        this.estado = EstadoNomina.PAGADA;
        this.jornales.forEach(Jornal::marcarComoLiquidado);
    }

    public boolean estaPendiente() {
        return EstadoNomina.PENDIENTE.equals(this.estado);
    }

    public List<Jornal> getJornales() {
        return Collections.unmodifiableList(jornales);
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Trabajador getTrabajador() { return trabajador; }
    public void setTrabajador(Trabajador trabajador) { this.trabajador = trabajador; }

    public LocalDate getPeriodoInicio() { return periodoInicio; }
    public void setPeriodoInicio(LocalDate periodoInicio) { this.periodoInicio = periodoInicio; }

    public LocalDate getPeriodoFin() { return periodoFin; }
    public void setPeriodoFin(LocalDate periodoFin) { this.periodoFin = periodoFin; }

    public void setJornales(List<Jornal> jornales) { this.jornales = jornales; }

    public int getTotalJornales() { return totalJornales; }
    public void setTotalJornales(int totalJornales) { this.totalJornales = totalJornales; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public EstadoNomina getEstado() { return estado; }
    public void setEstado(EstadoNomina estado) { this.estado = estado; }

    public LocalDate getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDate fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}