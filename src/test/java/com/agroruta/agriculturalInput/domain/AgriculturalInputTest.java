package com.agroruta.agriculturalInput.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas unitarias - AgriculturalInput")
class AgriculturalInputTest {

    private AgriculturalInput input;

    @BeforeEach
    void setUp() {
        input = AgriculturalInput.create(
                "Roundup",
                AgriculturalInputType.HERBICIDA,
                MeasurementUnit.LITROS,
                2.5,
                48
        );
    }

    // ── 1. Factory method create ─────────────────────────────────────────
    @Test
    @DisplayName("create debe asignar correctamente todos los campos")
    void create_debeAsignarTodosLosCampos() {
        assertEquals("Roundup",                       input.getNombre());
        assertEquals(AgriculturalInputType.HERBICIDA, input.getTipo());
        assertEquals(MeasurementUnit.LITROS,          input.getUnidadSugerida());
        assertEquals(2.5,                             input.getDosisSugerida());
        assertEquals(48,                              input.getReentradaHoras());
        assertTrue(input.isActivo());
        assertNotNull(input.getCreadoEn());
    }

    @Test
    @DisplayName("create debe registrar creadoEn con fecha cercana al momento actual")
    void create_debeFijarFechaCreacion() {
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);
        AgriculturalInput nuevo = AgriculturalInput.create(
                "Bayfolan", AgriculturalInputType.FERTILIZANTE_FOLIAR,
                MeasurementUnit.ML, 1.0, 0);
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);

        assertTrue(nuevo.getCreadoEn().isAfter(antes));
        assertTrue(nuevo.getCreadoEn().isBefore(despues));
    }

    // ── 2. Setters permitidos (id, activo, creadoEn) ─────────────────────
    @Test
    @DisplayName("setId debe actualizar el id")
    void setId_debeActualizarId() {
        input.setId(10L);
        assertEquals(10L, input.getId());
    }

    @Test
    @DisplayName("setActivo debe cambiar el estado activo")
    void setActivo_debeCambiarEstado() {
        input.setActivo(false);
        assertFalse(input.isActivo());

        input.setActivo(true);
        assertTrue(input.isActivo());
    }

    @Test
    @DisplayName("setCreadoEn debe actualizar la fecha de creación")
    void setCreadoEn_debeActualizarFecha() {
        LocalDateTime nueva = LocalDateTime.of(2024, 1, 15, 10, 0);
        input.setCreadoEn(nueva);
        assertEquals(nueva, input.getCreadoEn());
    }

    // ── 3. Método update ─────────────────────────────────────────────────
    @Test
    @DisplayName("update debe modificar todos los campos editables")
    void update_debeModificarTodosLosCampos() {
        input.update("Karate", AgriculturalInputType.INSECTICIDA,
                MeasurementUnit.ML, 0.5, 24);

        assertEquals("Karate",                         input.getNombre());
        assertEquals(AgriculturalInputType.INSECTICIDA, input.getTipo());
        assertEquals(MeasurementUnit.ML,                input.getUnidadSugerida());
        assertEquals(0.5,                               input.getDosisSugerida());
        assertEquals(24,                                input.getReentradaHoras());
    }

    @Test
    @DisplayName("update no debe alterar id, activo ni creadoEn")
    void update_noDebeAlterarCamposInmutables() {
        input.setId(5L);
        LocalDateTime fechaOriginal = input.getCreadoEn();

        input.update("Otro", AgriculturalInputType.FUNGICIDA,
                MeasurementUnit.KG, 1.0, 12);

        assertEquals(5L,           input.getId());
        assertTrue(input.isActivo());
        assertEquals(fechaOriginal, input.getCreadoEn());
    }

    // ── 4. Método deactivate ─────────────────────────────────────────────
    @Test
    @DisplayName("deactivate debe poner activo en false")
    void deactivate_debePonersActivo_enFalse() {
        assertTrue(input.isActivo());
        input.deactivate();
        assertFalse(input.isActivo());
    }

    @Test
    @DisplayName("deactivate aplicado dos veces debe mantener activo en false")
    void deactivate_aplicadoDoVeces_debeMantenerseFalse() {
        input.deactivate();
        input.deactivate();
        assertFalse(input.isActivo());
    }

    // ── 5. Método requiresReentryPeriod ──────────────────────────────────
    @Test
    @DisplayName("requiresReentryPeriod debe retornar true cuando reentradaHoras > 0")
    void requiresReentryPeriod_conHorasPositivas_debeRetornarTrue() {
        assertTrue(input.requiresReentryPeriod());
    }

    @Test
    @DisplayName("requiresReentryPeriod debe retornar false cuando reentradaHoras es 0")
    void requiresReentryPeriod_conCero_debeRetornarFalse() {
        AgriculturalInput sinReentrada = AgriculturalInput.create(
                "Calcio", AgriculturalInputType.FERTILIZANTE_SUELO,
                MeasurementUnit.KG, 3.0, 0);

        assertFalse(sinReentrada.requiresReentryPeriod());
    }

    @Test
    @DisplayName("requiresReentryPeriod debe retornar false cuando reentradaHoras es null")
    void requiresReentryPeriod_conNull_debeRetornarFalse() {
        AgriculturalInput sinHoras = AgriculturalInput.create(
                "Azufre", AgriculturalInputType.FUNGICIDA,
                MeasurementUnit.GRAMOS, 1.5, null);

        assertFalse(sinHoras.requiresReentryPeriod());
    }

    // ── 6. Enums ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("AgriculturalInputType debe contener todos los valores esperados y sus displayName")
    void agriculturalInputType_debeContenerTodosLosValores() {
        assertEquals(9, AgriculturalInputType.values().length);
        assertEquals("Fungicida",               AgriculturalInputType.FUNGICIDA.getDisplayName());
        assertEquals("Insecticida",             AgriculturalInputType.INSECTICIDA.getDisplayName());
        assertEquals("Herbicida",               AgriculturalInputType.HERBICIDA.getDisplayName());
        assertEquals("Fertilizante foliar",     AgriculturalInputType.FERTILIZANTE_FOLIAR.getDisplayName());
        assertEquals("Fertilizante de suelo",   AgriculturalInputType.FERTILIZANTE_SUELO.getDisplayName());
        assertEquals("Regulador de crecimiento",AgriculturalInputType.REGULADOR_CRECIMIENTO.getDisplayName());
        assertEquals("Coadyuvante",             AgriculturalInputType.COADYUVANTE.getDisplayName());
    }

    @Test
    @DisplayName("MeasurementUnit debe contener todos los valores esperados")
    void measurementUnit_debeContenerTodosLosValores() {
        assertEquals(4, MeasurementUnit.values().length);
        assertEquals(MeasurementUnit.LITROS,  MeasurementUnit.valueOf("LITROS"));
        assertEquals(MeasurementUnit.ML,      MeasurementUnit.valueOf("ML"));
        assertEquals(MeasurementUnit.GRAMOS,  MeasurementUnit.valueOf("GRAMOS"));
        assertEquals(MeasurementUnit.KG,      MeasurementUnit.valueOf("KG"));
    }
}