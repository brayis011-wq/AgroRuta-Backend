package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.Cargo;

import java.math.BigDecimal;

public class CargoResponse {
    public Long id;
    public String nombre;
    public String descripcion;
    public BigDecimal valorJornal;
    public boolean activo;

    public static CargoResponse from(Cargo c) {
        CargoResponse r = new CargoResponse();
        r.id = c.getId();
        r.nombre = c.getNombre();
        r.descripcion = c.getDescripcion();
        r.valorJornal = c.getValorJornal();
        r.activo = c.isActivo();
        return r;
    }
}