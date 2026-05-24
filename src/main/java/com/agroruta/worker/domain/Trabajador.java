package com.agroruta.worker.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
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
    private Cargo cargo;

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

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    public boolean estaActivo() {
        return EstadoTrabajador.ACTIVO.equals(this.estado);
    }

    public void desactivar()  { this.estado = EstadoTrabajador.INACTIVO; }
    public void suspender()   { this.estado = EstadoTrabajador.SUSPENDIDO; }
    public void reactivar()   { this.estado = EstadoTrabajador.ACTIVO; }
    public void cambiarCargo(Cargo nuevoCargo) { this.cargo = nuevoCargo; }
}