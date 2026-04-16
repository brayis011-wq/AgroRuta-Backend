package com.agroruta.worker.domain;

import java.time.LocalDate;

/**
 * Representa a un trabajador vinculado a la finca.
 * Puede estar asociado a uno o varios cultivos a través de sus jornales.
 */
public class Trabajador {

    private Long id;
    private String nombre;
    private String apellido;
    private String cedula;
    private String telefono;
    private String direccion;
    private LocalDate fechaIngreso;
    private EstadoTrabajador estado;
    private TipoContrato tipoContrato;
    private Cargo cargo;             // Cargo actual del trabajador

    public Trabajador() {}

    public Trabajador(Long id, String nombre, String apellido, String cedula,
                      String telefono, String direccion, LocalDate fechaIngreso,
                      TipoContrato tipoContrato, Cargo cargo) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.cedula = cedula;
        this.telefono = telefono;
        this.direccion = direccion;
        this.fechaIngreso = fechaIngreso;
        this.tipoContrato = tipoContrato;
        this.cargo = cargo;
        this.estado = EstadoTrabajador.ACTIVO;
    }

    // ── Lógica de dominio ────────────────────────────────────────────────────

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    public boolean estaActivo() {
        return EstadoTrabajador.ACTIVO.equals(this.estado);
    }

    public void desactivar() {
        this.estado = EstadoTrabajador.INACTIVO;
    }

    public void suspender() {
        this.estado = EstadoTrabajador.SUSPENDIDO;
    }

    public void reactivar() {
        this.estado = EstadoTrabajador.ACTIVO;
    }

    public void cambiarCargo(Cargo nuevoCargo) {
        this.cargo = nuevoCargo;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDate fechaIngreso) { this.fechaIngreso = fechaIngreso; }

    public EstadoTrabajador getEstado() { return estado; }
    public void setEstado(EstadoTrabajador estado) { this.estado = estado; }

    public TipoContrato getTipoContrato() { return tipoContrato; }
    public void setTipoContrato(TipoContrato tipoContrato) { this.tipoContrato = tipoContrato; }

    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }
}
