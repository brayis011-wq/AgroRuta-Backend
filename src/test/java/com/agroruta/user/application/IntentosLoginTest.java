package com.agroruta.user.application;

import com.agroruta.shared.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntentosLoginTest {

    /** Reloj que se puede adelantar a mano */
    static final class RelojMovible extends Clock {
        Instant ahora = Instant.parse("2026-11-05T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
        void adelantar(Duration d) { ahora = ahora.plus(d); }
    }

    private final RelojMovible reloj = new RelojMovible();
    private final IntentosLogin intentos = new IntentosLogin(reloj);

    private void fallar(String email, int veces) {
        for (int i = 0; i < veces; i++) intentos.registrarFallo(email);
    }

    @Test
    @DisplayName("cuatro fallos no bloquean, el quinto sí")
    void bloqueaAlQuinto() {
        fallar("ana@x.com", 4);
        assertDoesNotThrow(() -> intentos.exigirNoBloqueado("ana@x.com"));

        intentos.registrarFallo("ana@x.com");
        assertThrows(BusinessException.class, () -> intentos.exigirNoBloqueado("ana@x.com"));
    }

    @Test
    @DisplayName("el bloqueo no distingue mayúsculas ni espacios en el correo")
    void mismaClaveSinMayusculas() {
        fallar("Ana@X.com ", 5);
        assertThrows(BusinessException.class, () -> intentos.exigirNoBloqueado("ana@x.com"));
    }

    @Test
    @DisplayName("después de 15 minutos se puede volver a intentar")
    void seDesbloqueaConElTiempo() {
        fallar("ana@x.com", 5);
        reloj.adelantar(Duration.ofMinutes(15));
        assertDoesNotThrow(() -> intentos.exigirNoBloqueado("ana@x.com"));
    }

    @Test
    @DisplayName("los fallos viejos ya no cuentan")
    void fallosViejosNoCuentan() {
        fallar("ana@x.com", 4);
        reloj.adelantar(Duration.ofMinutes(16));
        intentos.registrarFallo("ana@x.com");
        assertDoesNotThrow(() -> intentos.exigirNoBloqueado("ana@x.com"));
    }

    @Test
    @DisplayName("un inicio de sesión correcto borra los fallos")
    void limpiarBorraLosFallos() {
        fallar("ana@x.com", 4);
        intentos.limpiar("ana@x.com");
        intentos.registrarFallo("ana@x.com");
        assertDoesNotThrow(() -> intentos.exigirNoBloqueado("ana@x.com"));
    }

    @Test
    @DisplayName("bloquear un correo no afecta a otro")
    void correosIndependientes() {
        fallar("ana@x.com", 5);
        assertDoesNotThrow(() -> intentos.exigirNoBloqueado("luis@x.com"));
    }
}
