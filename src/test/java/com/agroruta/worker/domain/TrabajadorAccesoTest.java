package com.agroruta.worker.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Trabajador - acceso al portal")
class TrabajadorAccesoTest {

    private Trabajador trabajador;

    @BeforeEach
    void setUp() {
        Cargo cargo = new Cargo(1L, "Recolector", "", new BigDecimal("60000"), true);
        trabajador = new Trabajador(1L, "Luis", "Herrera", "1052334120", null, null,
                LocalDate.of(2025, 1, 10), TipoContrato.JORNAL, cargo);
    }

    @Test
    @DisplayName("solicitarAcceso deja la solicitud pendiente con los datos de la cuenta")
    void solicitar() {
        trabajador.solicitarAcceso(9L, "Luis H.", "luis@gmail.com");

        assertThat(trabajador.getAccesoEstado()).isEqualTo(EstadoAccesoPortal.PENDIENTE);
        assertThat(trabajador.getUsuarioId()).isEqualTo(9L);
        assertThat(trabajador.getAccesoEmail()).isEqualTo("luis@gmail.com");
        assertThat(trabajador.tieneAccesoAprobado()).isFalse();
    }

    @Test
    @DisplayName("otra cuenta no puede pedir acceso si ya hay una solicitud en curso")
    void otraCuentaNoPuede() {
        trabajador.solicitarAcceso(9L, "Luis", "luis@gmail.com");

        assertThatThrownBy(() -> trabajador.solicitarAcceso(10L, "Intruso", "x@x.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("tras un rechazo, otra cuenta sí puede pedir acceso")
    void despuesDeRechazo() {
        trabajador.solicitarAcceso(9L, "Intruso", "x@x.com");
        trabajador.rechazarAcceso();

        trabajador.solicitarAcceso(10L, "Luis", "luis@gmail.com");

        assertThat(trabajador.getUsuarioId()).isEqualTo(10L);
        assertThat(trabajador.getAccesoEstado()).isEqualTo(EstadoAccesoPortal.PENDIENTE);
    }

    @Test
    @DisplayName("aprobar solo funciona con una solicitud pendiente")
    void aprobar() {
        assertThatThrownBy(() -> trabajador.aprobarAcceso()).isInstanceOf(IllegalStateException.class);

        trabajador.solicitarAcceso(9L, "Luis", "luis@gmail.com");
        trabajador.aprobarAcceso();

        assertThat(trabajador.tieneAccesoAprobado()).isTrue();
    }

    @Test
    @DisplayName("rechazar también retira un acceso ya aprobado")
    void retirarAcceso() {
        trabajador.solicitarAcceso(9L, "Luis", "luis@gmail.com");
        trabajador.aprobarAcceso();

        trabajador.rechazarAcceso();

        assertThat(trabajador.getAccesoEstado()).isEqualTo(EstadoAccesoPortal.RECHAZADO);
        assertThat(trabajador.tieneAccesoAprobado()).isFalse();
    }

    @Test
    @DisplayName("liberarAcceso borra el vínculo")
    void liberar() {
        trabajador.solicitarAcceso(9L, "Luis", "luis@gmail.com");
        trabajador.liberarAcceso();

        assertThat(trabajador.getUsuarioId()).isNull();
        assertThat(trabajador.getAccesoEstado()).isNull();
    }
}
