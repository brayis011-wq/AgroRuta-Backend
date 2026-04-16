package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.TipoContrato;

public class TrabajadorUpdateRequest {
    public String nombre;
    public String apellido;
    public String telefono;
    public String direccion;
    public TipoContrato tipoContrato;
}
