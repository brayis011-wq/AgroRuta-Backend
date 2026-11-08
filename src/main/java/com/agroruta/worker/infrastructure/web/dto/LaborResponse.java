package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.Labor;

public class LaborResponse {
    public Long id;
    public String nombre;
    public String descripcion;
    public boolean activa;
    /** true = labor común del sistema (el agricultor no la puede editar) */
    public boolean comun;

    public static LaborResponse from(Labor a) {
        LaborResponse r = new LaborResponse();
        r.id = a.getId();
        r.nombre = a.getNombre();
        r.descripcion = a.getDescripcion();
        r.activa = a.isActiva();
        r.comun = a.esComun();
        return r;
    }
}