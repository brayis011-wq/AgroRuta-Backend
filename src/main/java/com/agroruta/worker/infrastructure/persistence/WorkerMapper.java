package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.domain.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Convierte entre objetos de dominio y entidades JPA.
 * Sin dependencias de Spring — es puro Java.
 */
public class WorkerMapper {

    // ── Cargo ─────────────────────────────────────────────────────────────────

    public static Cargo toDomain(CargoEntity e) {
        if (e == null) return null;
        return new Cargo(e.getId(), e.getNombre(), e.getDescripcion(), e.getValorJornal(), e.isActivo());
    }

    public static CargoEntity toEntity(Cargo d) {
        if (d == null) return null;
        CargoEntity e = new CargoEntity();
        e.setId(d.getId());
        e.setNombre(d.getNombre());
        e.setDescripcion(d.getDescripcion());
        e.setValorJornal(d.getValorJornal());
        e.setActivo(d.isActivo());
        return e;
    }

    // ── Actividad ─────────────────────────────────────────────────────────────

    public static Actividad toDomain(ActividadEntity e) {
        if (e == null) return null;
        Actividad a = new Actividad(e.getId(), e.getNombre(), e.getDescripcion());
        a.setActiva(e.isActiva());
        return a;
    }

    public static ActividadEntity toEntity(Actividad d) {
        if (d == null) return null;
        ActividadEntity e = new ActividadEntity();
        e.setId(d.getId());
        e.setNombre(d.getNombre());
        e.setDescripcion(d.getDescripcion());
        e.setActiva(d.isActiva());
        return e;
    }

    // ── Trabajador ────────────────────────────────────────────────────────────

    public static Trabajador toDomain(TrabajadorEntity e) {
        if (e == null) return null;
        Trabajador t = new Trabajador(
                e.getId(), e.getNombre(), e.getApellido(), e.getCedula(),
                e.getTelefono(), e.getDireccion(), e.getFechaIngreso(),
                e.getTipoContrato(), toDomain(e.getCargo())
        );
        t.setEstado(e.getEstado());
        return t;
    }

    public static TrabajadorEntity toEntity(Trabajador d) {
        if (d == null) return null;
        TrabajadorEntity e = new TrabajadorEntity();
        e.setId(d.getId());
        e.setNombre(d.getNombre());
        e.setApellido(d.getApellido());
        e.setCedula(d.getCedula());
        e.setTelefono(d.getTelefono());
        e.setDireccion(d.getDireccion());
        e.setFechaIngreso(d.getFechaIngreso());
        e.setEstado(d.getEstado());
        e.setTipoContrato(d.getTipoContrato());
        e.setCargo(toEntity(d.getCargo()));
        return e;
    }

    // ── Jornal ────────────────────────────────────────────────────────────────

    public static Jornal toDomain(JornalEntity e) {
        if (e == null) return null;
        Jornal j = new Jornal(
                e.getId(), e.getFecha(), toDomain(e.getTrabajador()),
                e.getCultivoId(), e.getNombreCultivo(), e.getObservaciones()
        );
        j.setValorJornal(e.getValorJornal());
        j.setLiquidado(e.isLiquidado());
        List<Actividad> actividades = e.getActividades().stream()
                .map(WorkerMapper::toDomain)
                .collect(Collectors.toList());
        j.setActividades(actividades);
        return j;
    }

    public static JornalEntity toEntity(Jornal d) {
        if (d == null) return null;
        JornalEntity e = new JornalEntity();
        e.setId(d.getId());
        e.setFecha(d.getFecha());
        e.setTrabajador(toEntity(d.getTrabajador()));
        e.setCultivoId(d.getCultivoId());
        e.setNombreCultivo(d.getNombreCultivo());
        e.setObservaciones(d.getObservaciones());
        e.setValorJornal(d.getValorJornal());
        e.setLiquidado(d.isLiquidado());
        List<ActividadEntity> actividades = d.getActividades().stream()
                .map(WorkerMapper::toEntity)
                .collect(Collectors.toList());
        e.setActividades(actividades);
        return e;
    }

    // ── Nomina ────────────────────────────────────────────────────────────────

    public static Nomina toDomain(NominaEntity e) {
        if (e == null) return null;
        List<Jornal> jornales = e.getJornales().stream()
                .map(WorkerMapper::toDomain)
                .collect(Collectors.toList());
        Nomina n = new Nomina(
                e.getId(), toDomain(e.getTrabajador()),
                e.getPeriodoInicio(), e.getPeriodoFin(), jornales
        );
        n.setEstado(e.getEstado());
        n.setFechaGeneracion(e.getFechaGeneracion());
        n.setObservaciones(e.getObservaciones());
        return n;
    }

    public static NominaEntity toEntity(Nomina d) {
        if (d == null) return null;
        NominaEntity e = new NominaEntity();
        e.setId(d.getId());
        e.setTrabajador(toEntity(d.getTrabajador()));
        e.setPeriodoInicio(d.getPeriodoInicio());
        e.setPeriodoFin(d.getPeriodoFin());
        e.setTotalJornales(d.getTotalJornales());
        e.setValorTotal(d.getValorTotal());
        e.setEstado(d.getEstado());
        e.setFechaGeneracion(d.getFechaGeneracion());
        e.setObservaciones(d.getObservaciones());
        List<JornalEntity> jornales = d.getJornales().stream()
                .map(WorkerMapper::toEntity)
                .collect(Collectors.toList());
        e.setJornales(jornales);
        return e;
    }

    // ── Pago ──────────────────────────────────────────────────────────────────

    public static Pago toDomain(PagoEntity e) {
        if (e == null) return null;
        Pago p = new Pago(
                e.getId(), toDomain(e.getNomina()),
                e.getFechaPago(), e.getMonto(),
                e.getMetodoPago(), e.getComprobante()
        );
        p.setObservaciones(e.getObservaciones());
        return p;
    }

    public static PagoEntity toEntity(Pago d) {
        if (d == null) return null;
        PagoEntity e = new PagoEntity();
        e.setId(d.getId());
        e.setNomina(toEntity(d.getNomina()));
        e.setFechaPago(d.getFechaPago());
        e.setMonto(d.getMonto());
        e.setMetodoPago(d.getMetodoPago());
        e.setComprobante(d.getComprobante());
        e.setObservaciones(d.getObservaciones());
        return e;
    }
}
