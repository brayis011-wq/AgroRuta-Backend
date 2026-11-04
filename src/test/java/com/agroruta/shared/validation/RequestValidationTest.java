package com.agroruta.shared.validation;

import com.agroruta.crop.infrastructure.web.dto.ActualizarCentroideFincaRequest;
import com.agroruta.crop.infrastructure.web.dto.FincaRequest;
import com.agroruta.crop.infrastructure.web.dto.SiembraRequest;
import com.agroruta.user.infrastructure.web.dto.LoginRequest;
import com.agroruta.user.infrastructure.web.dto.RegisterRequest;
import com.agroruta.worker.infrastructure.web.dto.JornalRequest;
import com.agroruta.worker.infrastructure.web.dto.NominaRequest;
import com.agroruta.worker.infrastructure.web.dto.PagoRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica las reglas de Bean Validation de los DTOs de entrada.
 * Se prueba el DTO directamente, sin levantar Spring.
 */
@DisplayName("Validación de DTOs de entrada")
class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    private <T> Set<String> camposInvalidos(T dto) {
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        return violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    // ── Registro ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("RegisterRequest")
    class Registro {

        private RegisterRequest valido() {
            RegisterRequest r = new RegisterRequest();
            r.setNombre("Juan Pérez");
            r.setEmail("juan@agroruta.com");
            r.setPassword("password123");
            r.setRol("Agricultor");
            return r;
        }

        @Test
        @DisplayName("acepta un registro válido")
        void aceptaValido() {
            assertThat(camposInvalidos(valido())).isEmpty();
        }

        @Test
        @DisplayName("acepta registro sin rol (se asigna Agricultor)")
        void aceptaSinRol() {
            RegisterRequest r = valido();
            r.setRol(null);
            assertThat(camposInvalidos(r)).isEmpty();
        }

        @Test
        @DisplayName("rechaza correo con formato inválido")
        void rechazaCorreoInvalido() {
            RegisterRequest r = valido();
            r.setEmail("no-es-un-correo");
            assertThat(camposInvalidos(r)).containsExactly("email");
        }

        @Test
        @DisplayName("rechaza contraseña de menos de 8 caracteres")
        void rechazaPasswordCorta() {
            RegisterRequest r = valido();
            r.setPassword("1234567");
            assertThat(camposInvalidos(r)).containsExactly("password");
        }

        @Test
        @DisplayName("rechaza contraseña de más de 72 caracteres")
        void rechazaPasswordLarga() {
            RegisterRequest r = valido();
            r.setPassword("a".repeat(73));
            assertThat(camposInvalidos(r)).containsExactly("password");
        }

        @Test
        @DisplayName("rechaza campos vacíos")
        void rechazaVacios() {
            assertThat(camposInvalidos(new RegisterRequest()))
                    .containsExactlyInAnyOrder("nombre", "email", "password");
        }
    }

    // ── Login ────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("LoginRequest")
    class Login {

        @Test
        @DisplayName("no exige longitud mínima de contraseña (usuarios antiguos)")
        void noExigeMinimo() {
            LoginRequest r = new LoginRequest();
            r.setEmail("ana@agroruta.com");
            r.setPassword("abc");
            assertThat(camposInvalidos(r)).isEmpty();
        }

        @Test
        @DisplayName("rechaza correo y contraseña vacíos")
        void rechazaVacios() {
            LoginRequest r = new LoginRequest();
            r.setEmail(" ");
            r.setPassword("");
            assertThat(camposInvalidos(r)).containsExactlyInAnyOrder("email", "password");
        }
    }

    // ── Cultivos ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Cultivos")
    class Cultivos {

        @Test
        @DisplayName("rechaza finca con hectáreas negativas o en cero")
        void rechazaHectareasNoPositivas() {
            FincaRequest r = new FincaRequest();
            r.setNombre("La Esperanza");
            r.setUbicacion("Vereda El Monte");
            r.setAgricultorId(5L);

            r.setHectareas(-3.0);
            assertThat(camposInvalidos(r)).containsExactly("hectareas");

            r.setHectareas(0.0);
            assertThat(camposInvalidos(r)).containsExactly("hectareas");

            r.setHectareas(12.5);
            assertThat(camposInvalidos(r)).isEmpty();
        }

        @Test
        @DisplayName("rechaza siembra con fecha futura y plantas en cero")
        void rechazaSiembraInvalida() {
            SiembraRequest r = new SiembraRequest();
            r.setFechaSiembra(LocalDate.now().plusDays(5));
            r.setCantidadPlantas(0);
            r.setVariedad("COLOMBIA");
            r.setLoteId(3L);
            assertThat(camposInvalidos(r))
                    .containsExactlyInAnyOrder("fechaSiembra", "cantidadPlantas");
        }

        @Test
        @DisplayName("rechaza coordenadas fuera de rango")
        void rechazaCoordenadasFueraDeRango() {
            assertThat(camposInvalidos(new ActualizarCentroideFincaRequest(91.0, -181.0)))
                    .containsExactlyInAnyOrder("centroideLat", "centroideLng");
            assertThat(camposInvalidos(new ActualizarCentroideFincaRequest(4.57, -74.29)))
                    .isEmpty();
        }
    }

    // ── Trabajadores ─────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Trabajadores")
    class Trabajadores {

        @Test
        @DisplayName("rechaza nómina con fin anterior al inicio")
        void rechazaPeriodoInvertido() {
            NominaRequest r = new NominaRequest();
            r.trabajadorId = 1L;
            r.periodoInicio = LocalDate.of(2026, 10, 15);
            r.periodoFin = LocalDate.of(2026, 10, 1);
            assertThat(camposInvalidos(r)).containsExactly("periodoValido");
        }

        @Test
        @DisplayName("acepta nómina de un solo día")
        void aceptaPeriodoDeUnDia() {
            NominaRequest r = new NominaRequest();
            r.trabajadorId = 1L;
            r.periodoInicio = LocalDate.of(2026, 10, 1);
            r.periodoFin = LocalDate.of(2026, 10, 1);
            assertThat(camposInvalidos(r)).isEmpty();
        }

        @Test
        @DisplayName("rechaza jornal sin actividades")
        void rechazaJornalSinActividades() {
            JornalRequest r = new JornalRequest();
            r.trabajadorId = 1L;
            r.cultivoId = 2L;
            r.fecha = LocalDate.now();
            r.actividadIds = List.of();
            assertThat(camposInvalidos(r)).containsExactly("actividadIds");
        }

        @Test
        @DisplayName("rechaza pago con monto negativo")
        void rechazaMontoNegativo() {
            PagoRequest r = new PagoRequest();
            r.nominaId = 1L;
            r.monto = new BigDecimal("-1000");
            r.metodoPago = null;
            assertThat(camposInvalidos(r)).containsExactlyInAnyOrder("monto", "metodoPago");
        }
    }
}
