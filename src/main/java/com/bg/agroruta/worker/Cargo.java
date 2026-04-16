package com.bg.agroruta.worker;

import java.math.BigDecimal;

/**
 * Representa el cargo o rol que desempeña un trabajador en la finca.
 * Define el valor del jornal (pago por día) asociado al cargo.
 */
public class Cargo {

    private Long id;
    private String nombre;           // Ej: Recolector, Fumigador, Podador, Regador
    private String descripcion;
    private BigDecimal valorJornal;  // Precio por día de trabajo
    private boolean activo;

    public Cargo() {}

    public Cargo(Long id, String nombre, String descripcion, BigDecimal valorJornal, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.valorJornal = valorJornal;
        this.activo = activo;
    }

    // ── Lógica de dominio ────────────────────────────────────────────────────

    public void actualizar(String nombre, String descripcion, BigDecimal valorJornal) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.valorJornal = valorJornal;
    }

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getValorJornal() { return valorJornal; }
    public void setValorJornal(BigDecimal valorJornal) { this.valorJornal = valorJornal; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
