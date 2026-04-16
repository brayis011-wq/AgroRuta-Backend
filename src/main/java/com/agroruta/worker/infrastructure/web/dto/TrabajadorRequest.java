package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.TipoContrato;

import java.time.LocalDate;

public class TrabajadorRequest {
    public String nombre;
    public String apellido;
    public String cedula;
    public String telefono;
    public String direccion;
    public LocalDate fechaIngreso;
    public TipoContrato tipoContrato;
    public Long cargoId;
}