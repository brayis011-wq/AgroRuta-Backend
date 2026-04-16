package com.bg.agroruta.worker;

/**
 * Representa una actividad agrícola que puede realizarse en un jornal.
 * Ejemplos: Riego, Siembra, Cosecha, Fumigación, Poda, Abonado, etc.
 */
public class Actividad {

    private Long id;
    private String nombre;
    private String descripcion;
    private boolean activa;

    public Actividad() {}

    public Actividad(Long id, String nombre, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activa = true;
    }

    // ── Lógica de dominio ────────────────────────────────────────────────────

    public void desactivar() {
        this.activa = false;
    }

    public void activar() {
        this.activa = true;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
