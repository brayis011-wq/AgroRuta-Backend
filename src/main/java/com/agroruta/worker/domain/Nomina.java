package com.agroruta.worker.domain;

import com.agroruta.shared.exception.EstadoInvalidoException;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
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

    public void calcular() {
        this.totalJornales = this.jornales.size();
        this.valorTotal = this.jornales.stream()
                .map(Jornal::getValorJornal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void aprobar() {
        if (!EstadoNomina.PENDIENTE.equals(this.estado)) {
            throw new EstadoInvalidoException("Solo se pueden aprobar nóminas en estado PENDIENTE.");
        }
        this.estado = EstadoNomina.APROBADA;
    }

    public void anular() {
        if (EstadoNomina.PAGADA.equals(this.estado)) {
            throw new EstadoInvalidoException("No se puede anular una nómina ya pagada.");
        }
        this.estado = EstadoNomina.ANULADA;
    }

    public void reactivar() {
        if (!EstadoNomina.ANULADA.equals(this.estado)) {
            throw new EstadoInvalidoException("Solo se pueden reactivar nóminas en estado ANULADA.");
        }
        this.estado = EstadoNomina.PENDIENTE;
    }

    public void marcarComoPagada() {
        if (!EstadoNomina.APROBADA.equals(this.estado)) {
            throw new EstadoInvalidoException("Solo se pueden pagar nóminas en estado APROBADA.");
        }
        this.estado = EstadoNomina.PAGADA;
        this.jornales.forEach(Jornal::marcarComoLiquidado);
    }

    /**
     * Corrige una nómina pendiente quitándole un jornal (anotado por error o de otro periodo).
     * Una nómina no se deja sin jornales: en ese caso se elimina la nómina.
     */
    public void quitarJornal(Long jornalId) {
        if (!estaPendiente()) {
            throw new EstadoInvalidoException("Solo se pueden corregir nóminas pendientes. Si ya la aprobaste, anúlala y genérala de nuevo.");
        }
        if (jornales.size() == 1 && java.util.Objects.equals(jornales.get(0).getId(), jornalId)) {
            throw new EstadoInvalidoException("La nómina quedaría sin jornales: mejor elimínala.");
        }
        if (!jornales.removeIf(j -> java.util.Objects.equals(j.getId(), jornalId))) {
            throw new EstadoInvalidoException("Ese jornal no está en esta nómina.");
        }
        calcular();
    }

    /** Suma a una nómina pendiente jornales que se anotaron después de generarla. Devuelve cuántos sumó. */
    public int agregarJornales(List<Jornal> nuevos) {
        if (!estaPendiente()) {
            throw new EstadoInvalidoException("Solo se pueden corregir nóminas pendientes.");
        }
        int antes = jornales.size();
        for (Jornal j : nuevos) {
            boolean ya = jornales.stream().anyMatch(x -> java.util.Objects.equals(x.getId(), j.getId()));
            if (!ya) jornales.add(j);
        }
        calcular();
        return jornales.size() - antes;
    }

    /** Un jornal de la nómina pendiente se corrigió (p. ej. los kilos de una cosecha): se recalcula el total */
    public void reemplazarJornal(Jornal corregido) {
        if (!estaPendiente()) {
            throw new EstadoInvalidoException("Solo se pueden corregir nóminas pendientes.");
        }
        jornales.replaceAll(j -> java.util.Objects.equals(j.getId(), corregido.getId()) ? corregido : j);
        calcular();
    }

    /** El día cae dentro del periodo de la nómina */
    public boolean cubre(LocalDate dia) {
        return dia != null && !dia.isBefore(periodoInicio) && !dia.isAfter(periodoFin);
    }

    public boolean estaPendiente() {
        return EstadoNomina.PENDIENTE.equals(this.estado);
    }

    // Sobreescribe el getter de Lombok para proteger la lista
    public List<Jornal> getJornales() {
        return Collections.unmodifiableList(jornales);
    }
}