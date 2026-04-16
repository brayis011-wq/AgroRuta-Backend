package com.agroruta.worker.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa el registro diario de trabajo de un trabajador.
 * Es la pieza central del módulo: vincula al trabajador con el cultivo
 * y las actividades realizadas ese día.
 *
 * Un trabajador puede tener un jornal por día, pero ese jornal
 * puede contener múltiples actividades realizadas.
 */
public class Jornal {

    private Long id;
    private LocalDate fecha;
    private Trabajador trabajador;
    private Long cultivoId;              // Referencia al módulo de cultivos
    private String nombreCultivo;        // Desnormalizado para consultas rápidas
    private List<Actividad> actividades; // Actividades realizadas ese día
    private String observaciones;
    private BigDecimal valorJornal;      // Snapshot del valor al momento del registro
    private boolean liquidado;           // true si ya fue incluido en una nómina

    public Jornal() {
        this.actividades = new ArrayList<>();
        this.liquidado = false;
    }

    public Jornal(Long id, LocalDate fecha, Trabajador trabajador,
                  Long cultivoId, String nombreCultivo, String observaciones) {
        this.id = id;
        this.fecha = fecha;
        this.trabajador = trabajador;
        this.cultivoId = cultivoId;
        this.nombreCultivo = nombreCultivo;
        this.observaciones = observaciones;
        this.actividades = new ArrayList<>();
        this.liquidado = false;
        // Snapshot del valor del jornal según el cargo actual del trabajador
        this.valorJornal = trabajador.getCargo().getValorJornal();
    }

    // ── Lógica de dominio ────────────────────────────────────────────────────

    public void agregarActividad(Actividad actividad) {
        if (this.liquidado) {
            throw new IllegalStateException("No se pueden modificar actividades de un jornal ya liquidado.");
        }
        if (!this.actividades.contains(actividad)) {
            this.actividades.add(actividad);
        }
    }

    public void removerActividad(Actividad actividad) {
        if (this.liquidado) {
            throw new IllegalStateException("No se pueden modificar actividades de un jornal ya liquidado.");
        }
        this.actividades.remove(actividad);
    }

    public void marcarComoLiquidado() {
        this.liquidado = true;
    }

    public List<Actividad> getActividades() {
        return Collections.unmodifiableList(actividades);
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Trabajador getTrabajador() { return trabajador; }
    public void setTrabajador(Trabajador trabajador) { this.trabajador = trabajador; }

    public Long getCultivoId() { return cultivoId; }
    public void setCultivoId(Long cultivoId) { this.cultivoId = cultivoId; }

    public String getNombreCultivo() { return nombreCultivo; }
    public void setNombreCultivo(String nombreCultivo) { this.nombreCultivo = nombreCultivo; }

    public void setActividades(List<Actividad> actividades) { this.actividades = actividades; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public BigDecimal getValorJornal() { return valorJornal; }
    public void setValorJornal(BigDecimal valorJornal) { this.valorJornal = valorJornal; }

    public boolean isLiquidado() { return liquidado; }
    public void setLiquidado(boolean liquidado) { this.liquidado = liquidado; }
}
