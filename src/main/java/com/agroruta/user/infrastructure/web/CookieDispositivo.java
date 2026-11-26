package com.agroruta.user.infrastructure.web;

import com.agroruta.user.application.ports.in.SegundoFactorUseCase;
import jakarta.servlet.http.HttpServletResponse;
import com.agroruta.shared.config.SesionProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie de "dispositivo recordado": después de escribir bien el código de la verificación en dos pasos,
 * el login no lo vuelve a pedir en ese navegador durante 30 días.
 * Mismas protecciones que la cookie de sesión: httpOnly, SameSite=Strict y solo viaja a /api/auth.
 * Cerrar sesión no la borra (es justamente para la próxima vez que se entre).
 */
@Component
public class CookieDispositivo {

    public static final String NOMBRE = "agroruta_dispositivo";
    static final String RUTA = "/api/auth";

    private final boolean segura;

    public CookieDispositivo(SesionProperties propiedades) {
        this.segura = propiedades.cookieSegura();
    }

    public void escribir(HttpServletResponse response, String marca) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                base(marca).maxAge(SegundoFactorUseCase.RECORDAR_DISPOSITIVO).build().toString());
    }

    public void borrar(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, base("").maxAge(0).build().toString());
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOMBRE, valor)
                .httpOnly(true)
                .secure(segura)
                .sameSite("Strict")
                .path(RUTA);
    }
}
