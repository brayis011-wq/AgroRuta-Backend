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

    /** Id del usuario (agricultor) dueño de este trabajador. */
    private Long agricultorId;

    // ── Acceso al portal del trabajador ──────────────────────────────────────
    /** Cuenta de usuario (rol Trabajador) que pidió ver los datos de este trabajador. */
    private Long usuarioId;
    private EstadoAccesoPortal accesoEstado;
    /** Nombre y correo de la cuenta, para que el agricultor reconozca la solicitud. */
    private String accesoNombre;
    private String accesoEmail;

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

    // ── Acceso al portal ─────────────────────────────────────────────────────

    /** Una cuenta pide acceso a los datos de este trabajador. Queda pendiente de aprobación. */
    public void solicitarAcceso(Long usuarioId, String nombre, String email) {
        boolean ocupado = this.usuarioId != null && !this.usuarioId.equals(usuarioId)
                && (accesoEstado == EstadoAccesoPortal.PENDIENTE || accesoEstado == EstadoAccesoPortal.APROBADO);
        if (ocupado) {
            throw new IllegalStateException("Esta cédula ya tiene una cuenta vinculada o una solicitud en curso.");
        }
        if (this.usuarioId != null && this.usuarioId.equals(usuarioId) && accesoEstado == EstadoAccesoPortal.APROBADO) {
            return; // ya estaba aprobado para esta misma cuenta
        }
        this.usuarioId = usuarioId;
        this.accesoNombre = nombre;
        this.accesoEmail = email;
        this.accesoEstado = EstadoAccesoPortal.PENDIENTE;
    }

    public void aprobarAcceso() {
        if (accesoEstado != EstadoAccesoPortal.PENDIENTE) {
            throw new IllegalStateException("Este trabajador no tiene una solicitud de acceso pendiente.");
        }
        this.accesoEstado = EstadoAccesoPortal.APROBADO;
    }

    /** Rechaza una solicitud pendiente o retira un acceso ya aprobado. */
    public void rechazarAcceso() {
        if (accesoEstado != EstadoAccesoPortal.PENDIENTE && accesoEstado != EstadoAccesoPortal.APROBADO) {
            throw new IllegalStateException("Este trabajador no tiene acceso ni solicitudes pendientes.");
        }
        this.accesoEstado = EstadoAccesoPortal.RECHAZADO;
    }

    /** Quita el vínculo con la cuenta (la persona pidió acceso a otro trabajador). */
    public void liberarAcceso() {
        this.usuarioId = null;
        this.accesoEstado = null;
        this.accesoNombre = null;
        this.accesoEmail = null;
    }

    public boolean tieneAccesoAprobado() {
        return accesoEstado == EstadoAccesoPortal.APROBADO;
    }
}