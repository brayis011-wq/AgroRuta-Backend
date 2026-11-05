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
    /** Dueño de la labor. null = labor común (la crea el Administrador y la ven todos). */
    private Long agricultorId;

    public Actividad(Long id, String nombre, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activa = true;
    }

    /** Labor común del sistema: solo el Administrador la modifica. */
    public boolean esComun() { return agricultorId == null; }

    /** La puede usar en sus jornales: es común o es suya. */
    public boolean visiblePara(Long agricultorId) { return esComun() || java.util.Objects.equals(this.agricultorId, agricultorId); }

    public void desactivar() { this.activa = false; }
    public void activar()    { this.activa = true; }
}