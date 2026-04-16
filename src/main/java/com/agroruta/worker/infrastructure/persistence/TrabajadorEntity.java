package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.domain.EstadoTrabajador;
import com.agroruta.worker.domain.TipoContrato;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "trabajadores")
public class TrabajadorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 20)
    private String cedula;

    @Column(length = 20)
    private String telefono;

    @Column(length = 255)
    private String direccion;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTrabajador estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contrato", nullable = false, length = 20)
    private TipoContrato tipoContrato;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cargo_id", nullable = false)
    private CargoEntity cargo;

    public TrabajadorEntity() {}

    // Getters & Setters
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

    public CargoEntity getCargo() { return cargo; }
    public void setCargo(CargoEntity cargo) { this.cargo = cargo; }
}
