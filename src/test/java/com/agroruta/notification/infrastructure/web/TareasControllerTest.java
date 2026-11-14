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
        var r = new TareasController(alertas, limpieza,"clave-larga").alertasDiarias("clave-larga", false);
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsEntry("enviados", 4);
    }

    @Test
    @DisplayName("clave equivocada: 403; sin clave configurada: 404 y nunca corre")
    void sinPermiso() {
        assertThat(new TareasController(alertas, limpieza,"clave-larga").alertasDiarias("otra", false).getStatusCode().value()).isEqualTo(403);
        assertThat(new TareasController(alertas, limpieza,"clave-larga").alertasDiarias(null, false).getStatusCode().value()).isEqualTo(403);
        assertThat(new TareasController(alertas, limpieza,"").alertasDiarias("", false).getStatusCode().value()).isEqualTo(404);
        verify(alertas, never()).enviarResumenDelDia(false);
    }

    @Test
    @DisplayName("limpieza: con la clave correcta responde cuántas filas borró")
    void limpiezaConClave() {
        when(limpieza.limpiar()).thenReturn(Map.of("sesiones", 12));
        var r = new TareasController(alertas, limpieza, "clave-larga").limpiar("clave-larga");
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsEntry("sesiones", 12);
    }

    @Test
    @DisplayName("limpieza: clave equivocada 403, sin clave configurada 404 y nunca corre")
    void limpiezaSinPermiso() {
        assertThat(new TareasController(alertas, limpieza, "clave-larga").limpiar("otra").getStatusCode().value()).isEqualTo(403);
        assertThat(new TareasController(alertas, limpieza, "").limpiar("").getStatusCode().value()).isEqualTo(404);
        verify(limpieza, never()).limpiar();
    }
}
