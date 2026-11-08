package com.agroruta.shared.security;

import com.agroruta.user.infrastructure.security.CustomUserDetails;
import org.springframework.security.core.Authentication;

/**
 * Utilidades para saber quién hace la petición. Solo se usan en los controladores: ellos le pasan
 * el id del usuario a los casos de uso, así la lógica de negocio no depende de Spring Security.
 */
public final class CurrentUser {

    private static final String ROL_ADMIN = "ROLE_Administrador";

    private CurrentUser() {}

    public static boolean esAdmin(Authentication auth) {
        return auth != null && auth.isAuthenticated()
                && auth.getAuthorities().stream()
                .anyMatch(a -> ROL_ADMIN.equals(a.getAuthority()));
    }

    public static Long id(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails u) {
            return u.getId();
        }
        return null;
    }

    /** Nombre del usuario autenticado, o null si no hay sesión. */
    public static String nombre(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails u) {
            return u.getNombre();
        }
        return null;
    }
}
