package com.agroruta.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletRequest;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RutasAntiguasFilterTest {

    private static RutasAntiguasFilter.Traduccion t(String metodo, String ruta) {
        return RutasAntiguasFilter.traducir(metodo, ruta);
    }

    @Test
    void rutasActualesNoCambian() {
        assertThat(t("GET", "/api/farms/5")).isNull();
        assertThat(t("POST", "/api/auth/login")).isNull();
        assertThat(t("GET", "/api/backups/agroruta_2026.sql/download")).isNull();
        assertThat(t("GET", "/actuator/health")).isNull();
    }

    @Test
    void traduceSegmentoPorSegmento() {
        assertThat(t("GET", "/api/lotes/finca/7")).isEqualTo(new RutasAntiguasFilter.Traduccion("GET", "/api/plots/farm/7"));
        assertThat(t("GET", "/api/siembras/estado/ACTIVA").ruta()).isEqualTo("/api/plantings/status/ACTIVA");
        assertThat(t("PATCH", "/api/trabajadores/3/acceso/aprobar").ruta()).isEqualTo("/api/workers/3/access/approve");
        assertThat(t("POST", "/api/tareas/alertas-diarias").ruta()).isEqualTo("/api/jobs/daily-alerts");
        assertThat(t("GET", "/api/jornales/cultivo/9").ruta()).isEqualTo("/api/workdays/planting/9");
        assertThat(t("POST", "/api/auth/registro").ruta()).isEqualTo("/api/auth/register");
    }

    @Test
    void casosEspeciales() {
        assertThat(t("GET", "/api/v1/weather/city").ruta()).isEqualTo("/api/weather/city");
        assertThat(t("GET", "/api/worker/actividades").ruta()).isEqualTo("/api/labors");
    }

    @Test
    void cambiaElMetodoDondeLaApiCambio() {
        assertThat(t("PUT", "/api/siembras/4/avanzar-etapa"))
                .isEqualTo(new RutasAntiguasFilter.Traduccion("PATCH", "/api/plantings/4/advance-stage"));
        assertThat(t("DELETE", "/api/cargos/2"))
                .isEqualTo(new RutasAntiguasFilter.Traduccion("PATCH", "/api/positions/2/deactivate"));
        assertThat(t("DELETE", "/api/worker/actividades/8"))
                .isEqualTo(new RutasAntiguasFilter.Traduccion("PATCH", "/api/labors/8/deactivate"));
        // El borrado lógico de una siembra sigue siendo DELETE
        assertThat(t("DELETE", "/api/siembras/4"))
                .isEqualTo(new RutasAntiguasFilter.Traduccion("DELETE", "/api/plantings/4"));
    }

    @Test
    void laPeticionSigueConLaRutaYElMetodoNuevos() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("PUT", "/api/siembras/4/finalizar");
        req.setServletPath("/api/siembras/4/finalizar");
        AtomicReference<HttpServletRequest> recibida = new AtomicReference<>();

        new RutasAntiguasFilter().doFilter(req, new MockHttpServletResponse(),
                (r, s) -> recibida.set((HttpServletRequest) r));

        assertThat(recibida.get().getMethod()).isEqualTo("PATCH");
        assertThat(recibida.get().getRequestURI()).isEqualTo("/api/plantings/4/finish");
        assertThat(recibida.get().getServletPath()).isEqualTo("/api/plantings/4/finish");
        assertThat(recibida.get().getRequestURL().toString()).isEqualTo("http://localhost/api/plantings/4/finish");
    }
}
