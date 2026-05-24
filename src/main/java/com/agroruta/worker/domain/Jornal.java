package com.agroruta.worker.domain;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class Jornal {

    private Long id;
    private LocalDate fecha;
    private Trabajador trabajador;
    private Long cultivoId;
    private String nombreCultivo;
    private List<Actividad> actividades;
    private String observaciones;
    private BigDecimal valorJornal;
    private boolean liquidado;

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

    // Sobreescribe el getter de Lombok para proteger la lista
    public List<Actividad> getActividades() {
        return Collections.unmodifiableList(actividades);
    }
}