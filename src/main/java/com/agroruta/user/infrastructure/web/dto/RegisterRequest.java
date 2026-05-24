package com.agroruta.user.infrastructure.web.dto;
import lombok.Data;
@Data
public class RegisterRequest {
    private String nombre;
    private String email;
    private String password;
    private String rol;

}