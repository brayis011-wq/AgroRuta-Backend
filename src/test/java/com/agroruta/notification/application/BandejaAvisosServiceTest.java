package com.agroruta.notification.application;

import com.agroruta.notification.application.ports.out.BandejaAvisosRepository;
import com.agroruta.notification.domain.AvisoBandeja;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BandejaAvisosService")
class BandejaAvisosServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-18T12:00:00Z");
    private final BandejaAvisosRepository repo = mock(BandejaAvisosRepository.class);
    private final BandejaAvisosService servicio = new BandejaAvisosService(repo, Clock.fixed(AHORA, ZoneOffset.UTC));

    private AvisoBandeja.Nuevo aviso(Long usuario, String etiqueta) {
        return new AvisoBandeja.Nuevo(usuario, AvisoBandeja.Tipo.PAGO_REGISTRADO, "Te registraron un pago", "Recibiste $ 10",
                Map.of("monto", "10"), "/portal/payments", etiqueta);
    }

    @Test
    @DisplayName("guarda el aviso con la hora actual")
    void guarda() {
        servicio.avisar(aviso(5L, "pago-1"));
        verify(repo).guardar(aviso(5L, "pago-1"), AHORA);
    }

    @Test
    @DisplayName("un aviso incompleto no se guarda y un fallo de la base no se propaga")
    void tolerante() {
        servicio.avisar(aviso(null, "pago-1"));
        servicio.avisar(aviso(5L, null));
        verify(repo, never()).guardar(any(), any());

        doThrow(new RuntimeException("base caída")).when(repo).guardar(any(), any());
        assertThatCode(() -> servicio.avisar(aviso(5L, "pago-2"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("muestra los 30 más recientes")
    void recientes() {
        servicio.recientes(5L);
        verify(repo).recientes(5L, BandejaAvisosService.RECIENTES);
    }

    @Test
    @DisplayName("marcar leído un aviso ajeno o inexistente responde como no encontrado")
    void ajeno() {
        when(repo.marcarLeido(5L, 99L, AHORA)).thenReturn(false);
        assertThatThrownBy(() -> servicio.marcarLeido(5L, 99L)).isInstanceOf(ResourceNotFoundException.class);

        when(repo.marcarLeido(5L, 7L, AHORA)).thenReturn(true);
        assertThatCode(() -> servicio.marcarLeido(5L, 7L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("marcar todos leídos")
    void todos() {
        servicio.marcarTodosLeidos(5L);
        verify(repo).marcarTodosLeidos(5L, AHORA);
    }
}
