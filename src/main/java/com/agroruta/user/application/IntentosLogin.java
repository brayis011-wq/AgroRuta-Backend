package com.agroruta.user.application;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Frena la fuerza bruta en el inicio de sesión: después de 10 contraseñas equivocadas seguidas
 * para el mismo correo, ese correo queda bloqueado 15 minutos. Se guarda en memoria (si el
 * servidor se reinicia, los contadores vuelven a cero), suficiente para una sola instancia.
 */
@Component
public class IntentosLogin {

    static final int MAX_FALLOS = 10;
    static final Duration BLOQUEO = Duration.ofMinutes(15);
    private static final int LIMPIAR_DESDE = 10_000;

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final Clock reloj;

    @Autowired
    public IntentosLogin() {
        this(Clock.systemUTC());
    }

    IntentosLogin(Clock reloj) {
        this.reloj = reloj;
    }

    /** Lanza 429 si el correo está bloqueado por demasiados intentos. */
    public void exigirNoBloqueado(String email) {
        Registro r = registros.get(clave(email));
        if (r == null || r.bloqueadoHasta == null) return;
        Instant ahora = reloj.instant();
        if (ahora.isBefore(r.bloqueadoHasta)) {
            long minutos = Math.max(1, Duration.between(ahora, r.bloqueadoHasta).toMinutes() + 1);
            throw new BusinessException(ErrorCode.TOO_MANY_ATTEMPTS,
                    "Demasiados intentos fallidos. Espera " + minutos + " minutos e inténtalo de nuevo.");
        }
        registros.remove(clave(email));
    }

    /** Suma un fallo; al llegar a MAX_FALLOS, bloquea el correo. */
    public void registrarFallo(String email) {
        Instant ahora = reloj.instant();
        if (registros.size() > LIMPIAR_DESDE) {
            registros.values().removeIf(r -> r.vencido(ahora));
        }
        registros.compute(clave(email), (k, r) -> {
            Registro nuevo = (r == null || r.vencido(ahora)) ? new Registro(ahora) : r;
            nuevo.fallos++;
            if (nuevo.fallos >= MAX_FALLOS) {
                nuevo.bloqueadoHasta = ahora.plus(BLOQUEO);
            }
            return nuevo;
        });
    }

    /** Inicio de sesión correcto: se olvidan los fallos. */
    public void limpiar(String email) {
        registros.remove(clave(email));
    }

    private static String clave(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Registro {
        private final Instant primero;
        private int fallos;
        private Instant bloqueadoHasta;

        private Registro(Instant primero) {
            this.primero = primero;
        }

        /** Los fallos viejos (más de 15 min) ya no cuentan. */
        private boolean vencido(Instant ahora) {
            Instant fin = bloqueadoHasta != null ? bloqueadoHasta : primero.plus(BLOQUEO);
            return !ahora.isBefore(fin);
        }
    }
}
