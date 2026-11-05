package com.agroruta.notification.application;

import com.agroruta.crop.domain.AlertaCultivo;
import com.agroruta.crop.domain.AlertaCultivo.Tipo;
import com.agroruta.notification.application.MensajesCorreo.ClimaFinca;
import com.agroruta.notification.domain.Correo;
import com.agroruta.weather.domain.Pronostico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MensajesCorreo")
class MensajesCorreoTest {

    private final MensajesCorreo m = new MensajesCorreo("https://agroruta.app/");
    private final LocalDate hoy = LocalDate.of(2026, 11, 5);

    private AlertaCultivo a(Tipo t, String titulo, String detalle, LocalDate fecha) {
        return new AlertaCultivo(t, "La Esperanza", "Lote 2", "Uchuva · Colombia", titulo, detalle, fecha);
    }

    @Test
    @DisplayName("resumen diario: secciones, cada aviso y el enlace al calendario")
    void resumen() {
        Correo c = m.resumenDiario("Ana María Pérez", "ana@x.com", hoy, List.of(
                a(Tipo.FUMIGACION_ATRASADA, null, "3", hoy.minusDays(3)),
                a(Tipo.LABOR_MANANA, "RIEGO", "Goteo", hoy.plusDays(1)),
                a(Tipo.REENTRADA_EN_CURSO, "Mancozeb", "48", hoy.plusDays(1)),
                a(Tipo.CARENCIA_LIBRE, "Abamectina", null, hoy)),
                List.of(new ClimaFinca("La Esperanza", new Pronostico(12, 20, 8, 2))));

        assertThat(c.asunto()).isEqualTo("AgroRuta: 5 avisos para hoy");
        assertThat(c.texto()).contains("Hola Ana,", "jueves 5 de noviembre",
                "FUMIGACIONES", "fumigación atrasada 3 días (tocaba el 2 de noviembre)",
                "LABORES PROGRAMADAS", "mañana: Riego — Goteo.",
                "REENTRADA Y COSECHA", "no entrar hasta el 6 de noviembre (se aplicó Mancozeb)",
                "ya se puede cosechar", "CLIMA", "lluvia (8 mm): si fumigas",
                "https://agroruta.app/cultivo/calendario", "https://agroruta.app/configuracion");
    }

    @Test
    @DisplayName("lluvia sin fumigación pendiente y sin ser fuerte: no se menciona")
    void lluviaSinFumigar() {
        Correo c = m.resumenDiario("Ana", "ana@x.com", hoy, List.of(a(Tipo.LABOR_HOY, "PODA", null, hoy)),
                List.of(new ClimaFinca("La Esperanza", new Pronostico(12, 20, 8, 2))));
        assertThat(c.texto()).doesNotContain("CLIMA").contains("hoy: Poda.");
        assertThat(c.asunto()).isEqualTo("AgroRuta: 1 aviso para hoy");
    }

    @Test
    @DisplayName("pesos colombianos sin decimales")
    void pesos() {
        assertThat(MensajesCorreo.pesos(new BigDecimal("1250000.00"))).contains("1.250.000").doesNotContain(",00");
    }

    @Test
    @DisplayName("primer nombre para el saludo")
    void primerNombre() {
        assertThat(MensajesCorreo.primerNombre("  Ana María ")).isEqualTo("Ana");
        assertThat(MensajesCorreo.primerNombre(null)).isEmpty();
    }
}
