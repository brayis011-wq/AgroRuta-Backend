package com.agroruta.worker.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa la liquidación de un período de trabajo para un trabajador.
 * Agrupa todos los jornales del período y calcula el total a pagar.
 *
 * La nómina se genera automáticamente a partir de los jornales
 * no liquidados del trabajador en el rango de fechas indicado.
 */
public class Nomina {

    private Long id;
    private Trabajador trabajador;
    private LocalDate periodoInicio;
    private LocalDate periodoFin;
    private List<Jornal> jornales;   // Jornales incluidos en esta liquidación
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

    /**
     * Calcula el total de jornales y el valor a pagar
     * sumando el valorJornal (snapshot) de cada jornal registrado.
     */
    public void calcular() {
        this.totalJornales = this.jornales.size();
        this.valorTotal = this.jornales.stream()
                .map(Jornal::getValorJornal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void marcarComoPagada() {
        if (!EstadoNomina.PENDIENTE.equals(this.estado)) {
            throw new IllegalStateException("Solo se pueden pagar nóminas en estado PENDIENTE.");
        }
        this.estado = EstadoNomina.PAGADA;
        // Marcar todos los jornales como liquidados
        this.jornales.forEach(Jornal::marcarComoLiquidado);
    }

    public void anular() {
        if (EstadoNomina.PAGADA.equals(this.estado)) {
            throw new IllegalStateException("No se puede anular una nómina ya pagada.");
        }
        this.estado = EstadoNomina.ANULADA;
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
