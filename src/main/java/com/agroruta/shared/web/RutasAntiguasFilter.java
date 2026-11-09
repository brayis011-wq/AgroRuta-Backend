package com.agroruta.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * TEMPORAL: traduce las rutas viejas de la API (en español) a las nuevas (en inglés).
 *
 * <p>El frontend se publica en Cloudflare y el backend en Render, por separado, y quien tenga
 * la app abierta sigue usando el frontend anterior hasta que recargue. Mientras tanto este filtro
 * recibe, por ejemplo, {@code PUT /api/siembras/5/avanzar-etapa} y lo atiende como
 * {@code PATCH /api/plantings/5/advance-stage}. Corre antes que Spring Security, así que los
 * permisos se revisan sobre la ruta nueva.</p>
 *
 * <p>Se puede borrar cuando el log deje de mostrar "Ruta vieja de la API".</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RutasAntiguasFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RutasAntiguasFilter.class);

    /** Segmento viejo → segmento nuevo. Los que no están aquí (ids, nombres de archivo) no cambian. */
    static final Map<String, String> SEGMENTOS = Map.ofEntries(
            Map.entry("registro", "register"),
            Map.entry("verificar", "verify"),
            Map.entry("reenviar-verificacion", "resend-verification"),
            Map.entry("recuperar", "forgot-password"),
            Map.entry("restablecer", "reset-password"),
            Map.entry("resumen", "summary"),
            Map.entry("usuarios", "users"),
            Map.entry("estado", "status"),
            Map.entry("rol", "role"),
            Map.entry("agricultores", "farmers"),
            Map.entry("agricultor", "farmer"),
            Map.entry("todos", "all"),
            Map.entry("perfil", "profile"),
            Map.entry("actividades", "activities"),
            Map.entry("siembra", "planting"),
            Map.entry("siembras", "plantings"),
            Map.entry("catalogo-cultivos", "crop-catalog"),
            Map.entry("tipos", "types"),
            Map.entry("variedades", "varieties"),
            Map.entry("cosechas", "harvests"),
            Map.entry("costos", "costs"),
            Map.entry("calendario", "calendar"),
            Map.entry("fincas", "farms"),
            Map.entry("finca", "farm"),
            Map.entry("centroide", "centroid"),
            Map.entry("fumigaciones", "sprayings"),
            Map.entry("lotes", "plots"),
            Map.entry("lote", "plot"),
            Map.entry("geometria", "geometry"),
            Map.entry("historial", "history"),
            Map.entry("avanzar-etapa", "advance-stage"),
            Map.entry("retroceder-etapa", "revert-stage"),
            Map.entry("finalizar", "finish"),
            Map.entry("etapas", "stages"),
            Map.entry("inventario", "inventory"),
            Map.entry("avisos", "alerts"),
            Map.entry("movimientos", "movements"),
            Map.entry("compras", "purchases"),
            Map.entry("ajustes", "adjustments"),
            Map.entry("minimo", "minimum"),
            Map.entry("costo-estimado", "estimated-cost"),
            Map.entry("notificaciones", "notifications"),
            Map.entry("preferencias", "preferences"),
            Map.entry("tareas", "jobs"),
            Map.entry("alertas-diarias", "daily-alerts"),
            Map.entry("solicitud", "request"),
            Map.entry("jornales", "workdays"),
            Map.entry("nominas", "payrolls"),
            Map.entry("pagos", "payments"),
            Map.entry("cargos", "positions"),
            Map.entry("cargo", "position"),
            Map.entry("cultivos-disponibles", "available-plantings"),
            Map.entry("trabajador", "worker"),
            Map.entry("cultivo", "planting"),
            Map.entry("labores", "labors"),
            Map.entry("generar", "generate"),
            Map.entry("aprobar", "approve"),
            Map.entry("anular", "void"),
            Map.entry("reactivar", "reactivate"),
            Map.entry("trabajadores", "workers"),
            Map.entry("desactivar", "deactivate"),
            Map.entry("suspender", "suspend"),
            Map.entry("acceso", "access"),
            Map.entry("rechazar", "reject")
    );

    /** Cambios de estado que antes eran PUT y ahora son PATCH. */
    private static final Pattern PUT_A_PATCH =
            Pattern.compile("^/api/plantings/[^/]+/(advance-stage|revert-stage|finish)$");

    /** Cargos y labores se desactivaban con DELETE; ahora es PATCH /{id}/deactivate, como en trabajadores. */
    private static final Pattern DELETE_A_DESACTIVAR = Pattern.compile("^/api/(positions|labors)/[^/]+$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Traduccion t = traducir(request.getMethod(), request.getRequestURI());
        if (t == null) {
            chain.doFilter(request, response);
            return;
        }
        log.info("Ruta vieja de la API: {} {} → {} {}",
                request.getMethod(), request.getRequestURI(), t.metodo(), t.ruta());
        chain.doFilter(new Traducida(request, t), response);
    }

    record Traduccion(String metodo, String ruta) {}

    /** Devuelve la ruta nueva, o null si la petición ya usa una ruta actual. */
    static Traduccion traducir(String metodo, String ruta) {
        if (ruta == null || !ruta.startsWith("/api/")) return null;

        String nueva = ruta;
        if (nueva.startsWith("/api/worker/actividades")) {
            nueva = "/api/labores" + nueva.substring("/api/worker/actividades".length());
        }
        if (nueva.startsWith("/api/v1/weather")) {
            nueva = "/api/weather" + nueva.substring("/api/v1/weather".length());
        }
        nueva = Arrays.stream(nueva.split("/", -1))
                .map(s -> SEGMENTOS.getOrDefault(s, s))
                .collect(Collectors.joining("/"));
        if (nueva.equals(ruta)) return null;

        String nuevoMetodo = metodo;
        if ("PUT".equals(metodo) && PUT_A_PATCH.matcher(nueva).matches()) {
            nuevoMetodo = "PATCH";
        } else if ("DELETE".equals(metodo) && DELETE_A_DESACTIVAR.matcher(nueva).matches()) {
            nuevoMetodo = "PATCH";
            nueva = nueva + "/deactivate";
        }
        return new Traduccion(nuevoMetodo, nueva);
    }

    /** La misma petición, pero con la ruta y el método nuevos. */
    private static final class Traducida extends HttpServletRequestWrapper {
        private final Traduccion t;

        Traducida(HttpServletRequest request, Traduccion t) {
            super(request);
            this.t = t;
        }

        @Override public String getMethod() { return t.metodo(); }
        @Override public String getRequestURI() { return getContextPath() + t.ruta(); }
        @Override public String getServletPath() { return t.ruta(); }
        @Override public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer(getScheme()).append("://").append(getServerName());
            int puerto = getServerPort();
            if (puerto != 80 && puerto != 443) url.append(':').append(puerto);
            return url.append(getRequestURI());
        }
    }
}
