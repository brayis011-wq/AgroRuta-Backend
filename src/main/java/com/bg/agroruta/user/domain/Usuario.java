package com.bg.agroruta.user.domain;

import java.time.LocalDateTime;

public class Usuario {

    private Long id;
    private String nombre;
    private String email;
    private String password;
    private Rol rol;

    // Atributos profesionales para gestión de estado y auditoría
    private boolean activo;
    private LocalDateTime fechaCreacion;

    public Usuario(Long id, String nombre, String email, String password, Rol rol) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.rol = rol;
        this.activo = true; // Por defecto, un usuario nuevo nace activo
        this.fechaCreacion = LocalDateTime.now(); // Registramos cuándo se creó
    }

    public Usuario() {
    }

    // --- LÓGICA DE NEGOCIO (Comportamiento del Dominio) ---

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }

    public boolean isActivo() {
        return activo;
    }

    // --- GETTERS Y SETTERS BÁSICOS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}