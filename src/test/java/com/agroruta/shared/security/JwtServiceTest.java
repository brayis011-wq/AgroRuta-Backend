package com.agroruta.shared.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;

    // 32 bytes exactos en Base64 → válido para HMAC-SHA-256
    // Decodifica a: "abcdefghijklmnopqrstuvwxyz123456"
    private static final String SECRET = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=";
    private static final long EXPIRATION = 3_600_000L; // 1 hora

    /**
     * JWT almacena timestamps en SEGUNDOS (estándar Unix),
     * por lo que al serializar/deserializar se truncan hasta 999 ms.
     * Esta constante compensa esa pérdida en las aserciones de tiempo.
     */
    private static final long JWT_SECOND_TRUNCATION_MS = 1_000L;

    private UserDetails usuario;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);

        usuario = User.builder()
                .username("carlos@agroruta.com")
                .password("hashed-pwd")
                .authorities(Collections.emptyList())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // generateToken
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("genera un token no nulo ni vacío")
        void generaTokenNoNulo() {
            assertThat(jwtService.generateToken(usuario)).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("el token tiene exactamente 3 partes separadas por punto (header.payload.signature)")
        void tokenTieneTresPartes() {
            String token = jwtService.generateToken(usuario);
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("genera token con claims extra sin lanzar excepción")
        void generaTokenConClaimsExtra() {
            Map<String, Object> extra = Map.of("rol", "ADMIN", "finca", "El Rosal");
            assertThatCode(() -> jwtService.generateToken(extra, usuario))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("dos tokens generados con claims distintos son diferentes entre sí")
        void dosTokensDistintosConClaimsDiferentes() {
            String t1 = jwtService.generateToken(Map.of("a", "1"), usuario);
            String t2 = jwtService.generateToken(Map.of("a", "2"), usuario);
            assertThat(t1).isNotEqualTo(t2);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // extractUsername
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {

        @Test
        @DisplayName("extrae el username del token correctamente")
        void extraeUsernameCorrectamente() {
            String token = jwtService.generateToken(usuario);
            assertThat(jwtService.extractUsername(token)).isEqualTo("carlos@agroruta.com");
        }

        @Test
        @DisplayName("extrae el username de un token con claims extra")
        void extraeUsernameConClaimsExtra() {
            String token = jwtService.generateToken(Map.of("extra", "dato"), usuario);
            assertThat(jwtService.extractUsername(token)).isEqualTo("carlos@agroruta.com");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // extractClaim
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("extractClaim")
    class ExtractClaim {

        @Test
        @DisplayName("extrae el subject (username) vía lambda")
        void extraeSubject() {
            String token = jwtService.generateToken(usuario);
            String subject = jwtService.extractClaim(token, claims -> claims.getSubject());
            assertThat(subject).isEqualTo("carlos@agroruta.com");
        }

        @Test
        @DisplayName("extrae la fecha de expiración y es posterior a ahora")
        void extraeFechaExpiracionFutura() {
            String token = jwtService.generateToken(usuario);
            Date expiracion = jwtService.extractClaim(token, claims -> claims.getExpiration());
            assertThat(expiracion).isAfter(new Date());
        }

        /**
         * CORRECCIÓN: JWT serializa timestamps en segundos (estándar Unix), truncando
         * los milisegundos. Por ello, extractExpiration() puede devolver hasta 999 ms
         * menos que el valor pasado a new Date(System.currentTimeMillis() + expiration).
         *
         * Solución: restar JWT_SECOND_TRUNCATION_MS (1 000 ms) al límite inferior
         * para absorber esa pérdida de precisión sin afectar la validez del test.
         *
         * Ejemplo del problema original:
         *   Token generado en T=0   → expira en T+3 600 000 ms
         *   JWT guarda en segundos  → floor((T+3600000) / 1000) * 1000
         *   Si T tiene 656 ms frac. → el valor extraído es 344 ms menor que esperadoMin
         *   → isGreaterThanOrEqualTo fallaba con diferencia de -656 ms
         */
        @Test
        @DisplayName("la fecha de expiración está aproximadamente a 1 hora en el futuro")
        void fechaExpiracionEsAproximadamenteUnaHora() {
            long antes = System.currentTimeMillis();
            String token = jwtService.generateToken(usuario);
            Date expiracion = jwtService.extractClaim(token, claims -> claims.getExpiration());
            long despues = System.currentTimeMillis();

            // Límite inferior: resta 1 s para compensar la truncación a segundos de JWT
            long esperadoMin = antes + EXPIRATION - JWT_SECOND_TRUNCATION_MS;
            // Límite superior: el token no puede expirar más tarde que "despues + 1h"
            long esperadoMax = despues + EXPIRATION;

            assertThat(expiracion.getTime())
                    .isGreaterThanOrEqualTo(esperadoMin)
                    .isLessThanOrEqualTo(esperadoMax);
        }

        @Test
        @DisplayName("extrae la fecha de emisión (issuedAt) y es anterior o igual a ahora")
        void extraeFechaEmision() {
            long antes = System.currentTimeMillis();
            String token = jwtService.generateToken(usuario);
            Date emision = jwtService.extractClaim(token, claims -> claims.getIssuedAt());

            // issuedAt también se trunca a segundos → el valor puede ser hasta 999 ms
            // menor que "antes", por eso el margen inferior es antes - 1 s
            assertThat(emision.getTime())
                    .isGreaterThanOrEqualTo(antes - JWT_SECOND_TRUNCATION_MS)
                    .isLessThanOrEqualTo(System.currentTimeMillis());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // isTokenValid
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("token recién generado es válido para el mismo usuario")
        void tokenValidoParaMismoUsuario() {
            String token = jwtService.generateToken(usuario);
            assertThat(jwtService.isTokenValid(token, usuario)).isTrue();
        }

        @Test
        @DisplayName("token de un usuario NO es válido para otro usuario")
        void tokenNoValidoParaOtroUsuario() {
            String token = jwtService.generateToken(usuario);
            UserDetails otro = User.builder()
                    .username("otro@agroruta.com")
                    .password("pwd")
                    .authorities(Collections.emptyList())
                    .build();
            assertThat(jwtService.isTokenValid(token, otro)).isFalse();
        }

        @Test
        @DisplayName("token expirado lanza ExpiredJwtException al intentar validarlo")
        void tokenExpiradoLanzaExcepcion() {
            // Expiración en el pasado → JJWT lanza ExpiredJwtException al parsear
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
            String token = jwtService.generateToken(usuario);

            assertThatThrownBy(() -> jwtService.isTokenValid(token, usuario))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }
}