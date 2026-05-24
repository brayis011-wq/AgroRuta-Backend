package com.agroruta.user.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor  // ← genera el constructor con todos los campos
public class AuthResponse {
    private Long id;
    private String token;
    private String email;
    private String nombre;
    private String rol;
}