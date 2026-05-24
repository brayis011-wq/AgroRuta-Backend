package com.agroruta.worker.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Actividad {

    private Long id;
    private String nombre;
    private String descripcion;
    private boolean activa;

    public Actividad(Long id, String nombre, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activa = true;
    }

    public void desactivar() { this.activa = false; }
    public void activar()    { this.activa = true; }
}