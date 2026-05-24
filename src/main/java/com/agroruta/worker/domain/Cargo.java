package com.agroruta.worker.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class Cargo {

    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal valorJornal;
    private boolean activo;

    public Cargo(Long id, String nombre, String descripcion, BigDecimal valorJornal, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.valorJornal = valorJornal;
        this.activo = activo;
    }

    public void actualizar(String nombre, String descripcion, BigDecimal valorJornal) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.valorJornal = valorJornal;
    }

    public void desactivar() { this.activo = false; }
    public void activar()    { this.activo = true; }
}