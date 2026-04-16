package com.agroruta.user.infrastructure.web.dto;

public class AuthResponse {
    private Long id;
    private String token;
    private String email;
    private String nombre;
    private String rol;

    public AuthResponse(Long id, String token, String email, String nombre, String rol) {
        this.id = id;
        this.token = token;
        this.email = email;
        this.nombre = nombre;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public String getToken() { return token; }
    public String getEmail() { return email; }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
}