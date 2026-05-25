package com.agroruta.worker.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NominaTest {

    private Cargo cargo;
    private Trabajador trabajador;
    private Jornal jornal1;
    private Jornal jornal2;

    @BeforeEach
    void setUp() {
        cargo = new Cargo(1L, "Operario", "Trabajador de campo", new BigDecimal("50000"), true);
        trabajador = new Trabajador(1L, "Juan", "Pérez", "123456789",
                "3001234567", "Calle 1", LocalDate.now(),
                TipoContrato.JORNAL, cargo);
        jornal1 = new Jornal(1L, LocalDate.now(), trabajador, 10L, "Maíz", "Sin observaciones");
        jornal2 = new Jornal(2L, LocalDate.now().minusDays(1), trabajador, 10L, "Maíz", "Sin observaciones");
    }

    @Test
    void deberiaCrearNominaConValoresCorrectos() {
        Nomina nomina = new Nomina(1L, trabajador, LocalDate.now().minusDays(7),
                LocalDate.now(), List.of(jornal1, jornal2));

        assertEquals(1L, nomina.getId());
        assertEquals(trabajador, nomina.getTrabajador());
        assertEquals(EstadoNomina.PENDIENTE, nomina.getEstado());
        assertNotNull(nomina.getFechaGeneracion());
    }

    @Test
    void deberiaCalcularTotalJornalesYValor() {
        Nomina nomina = new Nomina(1L, trabajador, LocalDate.now().minusDays(7),
                LocalDate.now(), List.of(jornal1, jornal2));

        assertEquals(2, nomina.getTotalJornales());
        assertEquals(new BigDecimal("100000"), nomina.getValorTotal());
    }

    @Test
    void deberiaCrearNominaVaciaEnEstadoPendiente() {
        Nomina nomina = new Nomina();

        assertEquals(EstadoNomina.PENDIENTE, nomina.getEstado());
        assertNotNull(nomina.getJornales());
        assertTrue(nomina.getJornales().isEmpty());
    }

    @Test
    void deberiaAprobarNominaPendiente() {
        Nomina nomina = new Nomina(1L, trabajador, LocalDate.now().minusDays(7),
                LocalDate.now(), List.of(jornal1));

        nomina.aprobar();

        assertEquals(EstadoNomina.APROBADA, nomina.getEstado());
    }

    @Test
    void deberiaLanzarExcepcionAlAprobarNominaNoEstaEnPendiente() {
        Nomina nomina = new Nomina(1L, trabajador, LocalDate.now().minusDays(7),
                LocalDate.now(), List.of(jornal1));
        nomina.aprobar();

        assertThrows(IllegalStateException.class, nomina::aprobar);
    }

    @Test
    void deberiaAnularNomina() {
        Nomina nomina = new Nomina(1L, trabajador, LocalDate.now().minusDays(7),
                LocalDate.now(), List.of(jornal1));

        nomina.anular();

        assertEquals(EstadoNomina.ANULADA, nomina.getEstado());
    }

    @Test
    void deberiaLanzarExcepcionAlAnularNominaPagada() {
        Nomina nomina = new Nomina(1L, trabajador, LocalDate.now().minusDays(7),
                LocalDate.now(), List.of(jornal1));
        nomina.aprobar();
        nomina.marcarComoPagada();

        assertThrows(IllegalStateException.class, nomina::anular);
    }

    @Test
    void deberiaReactivarNominaAnulada() {
        Nomina nomina = new Nomina(1L, trabajador, LocalDate.now().minusDays(7),
                LocalDate.now(), List.of(jornal1));
        nomina.anular();

        nomina.reactivar();

        assertEquals(EstadoNomina.PENDIENTE, nomina.getEstado());
    }

    @Test
    void deberiaLanzarExcepcionAlReactivarNominaNoAnulada() {
        Nomina nomina = new Nomina(1L, trabajador, LocalDate.now().minusDays(7),
                LocalDate.now(), List.of(jornal1));

        assertThrows(IllegalStateException.class, nomina::reactivar);
    }

    @Test
    void deberiaMarcarComoPagadaYLiquidarJornales() {
        Nomina nomina = new Nomina(1L, trabajador, LocalDate.now().minusDays(7),
                LocalDate.now(), List.of(jornal1));
        nomina.aprobar();

        nomina.marcarComoPagada();

        assertEquals(EstadoNomina.PAGADA, nomina.getEstado());
        assertTrue(jornal1.isLiquidado());
    }

    @Test
    void deberiaLanzarExcepcionAlPagarNominaNoAprobada() {
        Nomina nomina = new Nomina(1L, trabajador, LocalDate.now().minusDays(7),
                LocalDate.now(), List.of(jornal1));

        assertThrows(IllegalStateException.class, nomina::marcarComoPagada);
    }

    @Test
    void deberiaRetornarTrueEnEstaPendiente() {
        Nomina nomina = new Nomina(1L, trabajador, LocalDate.now().minusDays(7),
                LocalDate.now(), List.of(jornal1));

        assertTrue(nomina.estaPendiente());
    }
}