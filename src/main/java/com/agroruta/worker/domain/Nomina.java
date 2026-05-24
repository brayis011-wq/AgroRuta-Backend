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

    // Sobreescribe el getter de Lombok para proteger la lista
    public List<Jornal> getJornales() {
        return Collections.unmodifiableList(jornales);
    }
}