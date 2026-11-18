package com.agroruta.notification.infrastructure.web;

import com.agroruta.notification.application.ports.in.BandejaAvisosUseCase;
import com.agroruta.notification.domain.AvisoBandeja;
import com.agroruta.user.infrastructure.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BandejaAvisosController")
class BandejaAvisosControllerTest {

    private final BandejaAvisosUseCase bandeja = mock(BandejaAvisosUseCase.class);
    private final BandejaAvisosController controller = new BandejaAvisosController(bandeja);

    private Authentication sesion(long id) {
        CustomUserDetails u = mock(CustomUserDetails.class);
        when(u.getId()).thenReturn(id);
        return new UsernamePasswordAuthenticationToken(u, null, List.of());
    }

    @Test
    @DisplayName("lista los avisos de quien inició sesión, con fecha ISO y si está leído")
    void listar() {
        when(bandeja.noLeidos(5L)).thenReturn(1);
        when(bandeja.recientes(5L)).thenReturn(List.of(new AvisoBandeja(7L, AvisoBandeja.Tipo.PAGO_REGISTRADO,
                "Te registraron un pago", "Recibiste $ 10", Map.of("monto", "10"), "/portal/payments",
                Instant.parse("2026-11-18T12:00:00Z"), null)));

        var r = controller.listar(sesion(5L)).getBody();

        assertThat(r.noLeidos()).isEqualTo(1);
        assertThat(r.avisos()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo("PAGO_REGISTRADO");
            assertThat(a.creadoEn()).isEqualTo("2026-11-18T12:00:00Z");
            assertThat(a.leido()).isFalse();
            assertThat(a.datos()).containsEntry("monto", "10");
        });
    }

    @Test
    @DisplayName("el contador, marcar uno y marcar todos usan el id de la sesión")
    void acciones() {
        when(bandeja.noLeidos(5L)).thenReturn(3);
        assertThat(controller.noLeidos(sesion(5L)).getBody()).containsEntry("noLeidos", 3);

        assertThat(controller.marcarLeido(7L, sesion(5L)).getStatusCode().value()).isEqualTo(204);
        verify(bandeja).marcarLeido(5L, 7L);

        assertThat(controller.marcarTodos(sesion(5L)).getStatusCode().value()).isEqualTo(204);
        verify(bandeja).marcarTodosLeidos(5L);
    }
}
