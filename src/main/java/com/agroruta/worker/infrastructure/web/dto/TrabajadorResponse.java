package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.EstadoTrabajador;
import com.agroruta.worker.domain.TipoContrato;
import com.agroruta.worker.domain.Trabajador;

import java.time.LocalDate;

public class TrabajadorResponse {
    public Long id;
    public String nombre;
    public String apellido;
    public String nombreCompleto;
    public String cedula;
    public String telefono;
    public String direccion;
    public LocalDate fechaIngreso;
    public EstadoTrabajador estado;
    public TipoContrato tipoContrato;
    public CargoResponse cargo;

    public static TrabajadorResponse from(Trabajador t) {
        TrabajadorResponse r = new TrabajadorResponse();
        r.id = t.getId();
        r.nombre = t.getNombre();
        r.apellido = t.getApellido();
        r.nombreCompleto = t.getNombreCompleto();
        r.cedula = t.getCedula();
        r.telefono = t.getTelefono();
        r.direccion = t.getDireccion();
        r.fechaIngreso = t.getFechaIngreso();
        r.estado = t.getEstado();
        r.tipoContrato = t.getTipoContrato();
        r.cargo = CargoResponse.from(t.getCargo());
        return r;
    }
}
