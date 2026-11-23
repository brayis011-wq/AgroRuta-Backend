package com.agroruta.notification.infrastructure.web;

import com.agroruta.notification.application.ports.in.AlertasDiariasUseCase;
import com.agroruta.shared.maintenance.LimpiezaDatosVencidos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TareasController")
class TareasControllerTest {

    private final AlertasDiariasUseCase alertas = mock(AlertasDiariasUseCase.class);
    private final LimpiezaDatosVencidos limpieza = mock(LimpiezaDatosVencidos.class);

    @Test
    @DisplayName("con la clave correcta manda el resumen")
    void claveCorrecta() {
        when(alertas.enviarResumenDelDia(false)).thenReturn(4);
        var r = new TareasController(alertas, limpieza, null, "clave-larga").alertasDiarias("clave-larga", false);
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsEntry("enviados", 4);
    }

    @Test
    @DisplayName("clave equivocada: 403; sin clave configurada: 404 y nunca corre")
    void sinPermiso() {
        assertThat(new TareasController(alertas, limpieza, null, "clave-larga").alertasDiarias("otra", false).getStatusCode().value()).isEqualTo(403);
        assertThat(new TareasController(alertas, limpieza, null, "clave-larga").alertasDiarias(null, false).getStatusCode().value()).isEqualTo(403);
        assertThat(new TareasController(alertas, limpieza, null, "").alertasDiarias("", false).getStatusCode().value()).isEqualTo(404);
        verify(alertas, never()).enviarResumenDelDia(false);
    }

    @Test
    @DisplayName("limpieza: con la clave correcta responde cuántas filas borró")
    void limpiezaConClave() {
        when(limpieza.limpiar()).thenReturn(Map.of("sesiones", 12));
        var r = new TareasController(alertas, limpieza, null, "clave-larga").limpiar("clave-larga");
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsEntry("sesiones", 12);
    }

    @Test
    @DisplayName("precios subidos desde el PC: con la clave se guardan, comprimidos o no")
    void subirPrecios() throws Exception {
        var precios = mock(com.agroruta.finance.application.ports.in.PreciosReferenciaUseCase.class);
        java.util.List<String> leidos = new java.util.ArrayList<>();
        when(precios.cargarRespuesta(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> {
            leidos.add(new String(((java.io.InputStream) inv.getArgument(0)).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            return new com.agroruta.finance.domain.EstadoPrecios(true, java.time.LocalDate.of(2026, 11, 14),
                    java.time.LocalDateTime.of(2026, 11, 23, 7, 0), false, null, null);
        });
        var c = new TareasController(alertas, limpieza, precios, "clave-larga");

        var plano = new org.springframework.mock.web.MockHttpServletRequest();
        plano.setContent("<xml/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var r = c.subirPrecios("clave-larga", plano);
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsEntry("semana", "2026-11-14");

        var buf = new java.io.ByteArrayOutputStream();
        try (var gz = new java.util.zip.GZIPOutputStream(buf)) { gz.write("<comprimido/>".getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
        var comprimido = new org.springframework.mock.web.MockHttpServletRequest();
        comprimido.setContent(buf.toByteArray());
        c.subirPrecios("clave-larga", comprimido);
        assertThat(leidos).containsExactly("<xml/>", "<comprimido/>");
    }

    @Test
    @DisplayName("precios subidos desde el PC: sin la clave no se lee nada")
    void subirPreciosSinPermiso() throws Exception {
        var precios = mock(com.agroruta.finance.application.ports.in.PreciosReferenciaUseCase.class);
        var pedido = new org.springframework.mock.web.MockHttpServletRequest();
        pedido.setContent("<xml/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(new TareasController(alertas, limpieza, precios, "clave-larga").subirPrecios("otra", pedido).getStatusCode().value()).isEqualTo(403);
        assertThat(new TareasController(alertas, limpieza, precios, "").subirPrecios("", pedido).getStatusCode().value()).isEqualTo(404);
        verify(precios, never()).cargarRespuesta(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("limpieza: clave equivocada 403, sin clave configurada 404 y nunca corre")
    void limpiezaSinPermiso() {
        assertThat(new TareasController(alertas, limpieza, null, "clave-larga").limpiar("otra").getStatusCode().value()).isEqualTo(403);
        assertThat(new TareasController(alertas, limpieza, null, "").limpiar("").getStatusCode().value()).isEqualTo(404);
        verify(limpieza, never()).limpiar();
    }
}
