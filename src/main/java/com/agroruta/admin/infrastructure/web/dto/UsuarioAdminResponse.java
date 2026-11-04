package com.agroruta.admin.infrastructure.web.dto;

import com.agroruta.user.domain.Usuario;

import java.time.LocalDateTime;

/** Datos de un usuario para el panel de administración (sin contraseña). */
public record UsuarioAdminResponse(
        Long id,
        String nombre,
        String email,
        String rol,
        boolean activo,
        String telefono,
        LocalDateTime fechaCreacion
) {
    public static UsuarioAdminResponse from(Usuario u) {
        return new UsuarioAdminResponse(
                u.getId(), u.getNombre(), u.getEmail(), u.getRol().name(),
                u.isActivo(), u.getTelefono(), u.getFechaCreacion());
    }
}
