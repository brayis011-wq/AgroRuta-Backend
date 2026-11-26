package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.ModalidadPago;
import com.agroruta.worker.domain.PlanillaSemanal;
import com.agroruta.worker.domain.Trabajador;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** GET /api/workdays/sheet: lo justo para pintar la cuadrícula trabajadores × días. */
public record PlanillaResponse(LocalDate inicio, LocalDate fin, List<Fila> trabajadores, List<Casilla> jornales) {

    public record Fila(
            Long id,
            String nombreCompleto,
            String iniciales,
            boolean activo,
            String cargo,
            ModalidadPago modalidadPago,
            BigDecimal valorJornal,
            BigDecimal valorKilo) {}

    public record Casilla(
            Long id,
            Long trabajadorId,
            LocalDate fecha,
            Long cultivoId,
            String nombreCultivo,
            BigDecimal valor,
            BigDecimal kilos,
            boolean liquidado,
            boolean enNomina,
            /** PENDIENTE (se puede corregir), APROBADA, PAGADA, ANULADA o null */
            com.agroruta.worker.domain.EstadoNomina nominaEstado) {}

    public static PlanillaResponse from(PlanillaSemanal p) {
        List<Fila> filas = p.trabajadores().stream().map(PlanillaResponse::fila).toList();
        List<Casilla> casillas = p.jornales().stream()
                .map(j -> new Casilla(
                        j.getId(),
                        j.getTrabajador() == null ? null : j.getTrabajador().getId(),
                        j.getFecha(),
                        j.getCultivoId(),
                        j.getNombreCultivo(),
                        j.getValorJornal(),
                        j.getKilos(),
                        j.isLiquidado(),
                        p.enNomina().containsKey(j.getId()),
                        p.enNomina().get(j.getId())))
                .toList();
        return new PlanillaResponse(p.inicio(), p.fin(), filas, casillas);
    }

    private static Fila fila(Trabajador t) {
        var c = t.getCargo();
        return new Fila(
                t.getId(),
                t.getNombreCompleto(),
                t.getIniciales(),
                t.estaActivo(),
                c == null ? null : c.getNombre(),
                c == null || c.getModalidadPago() == null ? ModalidadPago.JORNAL : c.getModalidadPago(),
                c == null ? null : c.getValorJornal(),
                c == null ? null : c.getValorKilo());
    }
}
