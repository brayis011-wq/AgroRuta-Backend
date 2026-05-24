package com.agroruta.configuration.infrastructure.web.dto;

import com.agroruta.configuration.domain.Perfil;
import com.agroruta.user.domain.Rol;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileResponse {

    private Long   id;
    private String nombre;
    private String email;
    private String telefono;
    private String fotoPerfil;
    private Rol    rol;

    public static ProfileResponse from(Perfil p) {
        return ProfileResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .email(p.getEmail())
                .telefono(p.getTelefono())
                .fotoPerfil(p.getFotoPerfil())
                .rol(p.getRol())
                .build();
    }
}