package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.Actividad;

public class ActividadResponse {
    public Long id;
    public String nombre;
    public String descripcion;
    public boolean activa;
    /** true = labor común del sistema (el agricultor no la puede editar) */
    public boolean comun;

    public static ActividadResponse from(Actividad a) {
        ActividadResponse r = new ActividadResponse();
        r.id = a.getId();
        r.nombre = a.getNombre();
        r.descripcion = a.getDescripcion();
        r.activa = a.isActiva();
        r.comun = a.esComun();
        return r;
    }
}