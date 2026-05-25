package com.agroruta.worker.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PagoTest {

    private Cargo cargo;
    private Trabajador trabajador;
    private Jornal jornal;
    private Nomina nomina;

    @BeforeEach
    void setUp() {
        cargo = new Cargo(1L, "Operario", "Trabajador de campo", new BigDecimal("50000"), true);
        trabajador = new Trabajador(1L, "Juan", "Pérez", "123456789",
                "3001234567", "Calle 1", LocalDate.now(),
                TipoContrato.JORNAL, cargo);
        jornal = new Jornal(1L, LocalDate.now(), trabajador, 10L, "Maíz", "Sin observaciones");
        nomina = new Nomina(1L, trabajador, LocalDate.now().minusDays(7),
                LocalDate.now(), List.of(jornal));
    }

    @Test
    void deberiaCrearPagoConValoresCorrectos() {
        Pago pago = new Pago(1L, nomina, LocalDate.now(),
                new BigDecimal("50000"), MetodoPago.EFECTIVO, "COMP-001");

        assertEquals(1L, pago.getId());
        assertEquals(nomina, pago.getNomina());
        assertEquals(LocalDate.now(), pago.getFechaPago());
        assertEquals(new BigDecimal("50000"), pago.getMonto());
        assertEquals(MetodoPago.EFECTIVO, pago.getMetodoPago());
        assertEquals("COMP-001", pago.getComprobante());
    }

    @Test
    void deberiaRetornarTrueSiMontoEsCorrecto() {
        Pago pago = new Pago(1L, nomina, LocalDate.now(),
                new BigDecimal("50000"), MetodoPago.TRANSFERENCIA, "COMP-001");

        assertTrue(pago.montoEsCorrecto());
    }

    @Test
    void deberiaRetornarFalseSiMontoEsIncorrecto() {
        Pago pago = new Pago(1L, nomina, LocalDate.now(),
                new BigDecimal("30000"), MetodoPago.CHEQUE, "COMP-001");

        assertFalse(pago.montoEsCorrecto());
    }

    @Test
    void deberiaCrearPagoVacioConNoArgsConstructor() {
        Pago pago = new Pago();

        assertNull(pago.getId());
        assertNull(pago.getNomina());
        assertNull(pago.getMonto());
    }

    @Test
    void deberiaPermitirCambiarMetodoPago() {
        Pago pago = new Pago(1L, nomina, LocalDate.now(),
                new BigDecimal("50000"), MetodoPago.EFECTIVO, "COMP-001");

        pago.setMetodoPago(MetodoPago.TRANSFERENCIA);

        assertEquals(MetodoPago.TRANSFERENCIA, pago.getMetodoPago());
    }

    @Test
    void deberiaPermitirAgregarObservaciones() {
        Pago pago = new Pago(1L, nomina, LocalDate.now(),
                new BigDecimal("50000"), MetodoPago.EFECTIVO, "COMP-001");

        pago.setObservaciones("Pago realizado a tiempo");

        assertEquals("Pago realizado a tiempo", pago.getObservaciones());
    }
}