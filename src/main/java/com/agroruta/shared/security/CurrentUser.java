package com.agroruta.shared.security;

import com.agroruta.user.infrastructure.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Utilidades para saber quién hace la petición. */
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

    /** Id del usuario autenticado en el hilo actual, o null si no hay sesión. */
    public static Long idActual() {
        return id(SecurityContextHolder.getContext().getAuthentication());
    }
}