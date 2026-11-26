package com.agroruta.worker.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * La cuadrícula de jornales de una semana: los trabajadores activos (y los que tuvieron jornal
 * esos días aunque ya no estén activos) y los jornales de cada uno.
 *
 * @param enNomina estado de la nómina de cada jornal que está en una (los pendientes se pueden corregir)
 */
public record PlanillaSemanal(
        LocalDate inicio,
        LocalDate fin,
        List<Trabajador> trabajadores,
        List<Jornal> jornales,
        java.util.Map<Long, EstadoNomina> enNomina) {

    /** Un jornal para anotar desde la planilla. kilos solo para los cargos que se pagan por kilo. */
    public record Entrada(Long trabajadorId, LocalDate fecha, java.math.BigDecimal kilos) {}
}
