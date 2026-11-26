package com.agroruta.shared.security;

import com.agroruta.shared.config.SesionProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie con el token de sesión renovable.
 * <ul>
 *   <li>httpOnly: JavaScript no puede leerla, así que un XSS no puede robarla.</li>
 *   <li>SameSite=Strict: el navegador no la manda desde otros sitios (sin CSRF).</li>
 *   <li>Path=/api/auth: solo viaja a renovar o cerrar la sesión, no en cada petición.</li>
 * </ul>
 * En producción el frontend llega a la API por el proxy del Worker de Cloudflare, así que la cookie
 * es del mismo sitio (app.agroruta.workers.dev).
 */
@Component
public class CookieSesion {

    public static final String NOMBRE = "agroruta_sesion";
    static final String RUTA = "/api/auth";

    private final boolean segura;
    private final Duration duracion;

    public CookieSesion(SesionProperties propiedades) {
        this.segura = propiedades.cookieSegura();
        this.duracion = propiedades.duracion();
    }

    public void escribir(HttpServletResponse response, String token) {
        response.addHeader(
                HttpHeaders.SET_COOKIE, base(token).maxAge(duracion).build().toString());
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
