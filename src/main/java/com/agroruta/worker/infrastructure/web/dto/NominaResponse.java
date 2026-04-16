package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.Nomina;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class NominaResponse {
    public Long id;
    public TrabajadorResponse trabajador;
    public LocalDate periodoInicio;
    public LocalDate periodoFin;
    public int totalJornales;
    public BigDecimal valorTotal;
    public EstadoNomina estado;
    public LocalDate fechaGeneracion;
    public String observaciones;
    public List<JornalResponse> jornales;

    public static NominaResponse from(Nomina n) {
        NominaResponse r = new NominaResponse();
        r.id = n.getId();
        r.trabajador = TrabajadorResponse.from(n.getTrabajador());
        r.periodoInicio = n.getPeriodoInicio();
        r.periodoFin = n.getPeriodoFin();
        r.totalJornales = n.getTotalJornales();
        r.valorTotal = n.getValorTotal();
        r.estado = n.getEstado();
        r.fechaGeneracion = n.getFechaGeneracion();
        r.observaciones = n.getObservaciones();
        r.jornales = n.getJornales().stream().map(JornalResponse::from).toList();
        return r;
    }
}