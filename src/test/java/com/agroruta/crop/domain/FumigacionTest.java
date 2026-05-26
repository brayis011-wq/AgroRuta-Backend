package com.agroruta.crop.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class FumigacionTest {

    private static final LocalDate FECHA = LocalDate.of(2024, 6, 15);
    private static final String PRODUCTO = "Glifosato";
    private static final Long INSUMO_ID = 10L;
    private static final Double DOSIS = 2.5;
    private static final UnidadMedida UNIDAD = UnidadMedida.LITROS;
    private static final Double AREA = 5.0;
    private static final String OBSERVACIONES = "Aplicación en zona norte";
    private static final Long SIEMBRA_ID = 99L;

    // ── fromCatalog ──────────────────────────────────────────────

    @Test
    void deberiaCrearFumigacionDesdeCatalogoConTodosLosCampos() {
        Fumigacion f = Fumigacion.fromCatalog(
                FECHA, PRODUCTO, INSUMO_ID, DOSIS,
                UNIDAD, AREA, OBSERVACIONES, SIEMBRA_ID);

        assertEquals(FECHA, f.getFecha());
        assertEquals(PRODUCTO, f.getProducto());
        assertEquals(INSUMO_ID, f.getAgriculturalInputId());
        assertEquals(DOSIS, f.getDosis());
        assertEquals(UNIDAD, f.getUnidadMedida());
        assertEquals(AREA, f.getAreaAplicada());
        assertEquals(OBSERVACIONES, f.getObservaciones());
        assertEquals(SIEMBRA_ID, f.getSiembraId());
    }

    @Test
    void fromCatalogoDeberiaDejarIdNulo() {
        Fumigacion f = Fumigacion.fromCatalog(
                FECHA, PRODUCTO, INSUMO_ID, DOSIS,
                UNIDAD, AREA, OBSERVACIONES, SIEMBRA_ID);

        assertNull(f.getId());
    }

    // ── fromManualEntry ──────────────────────────────────────────

    @Test
    void deberiaCrearFumigacionManualSinInsumoDelCatalogo() {
        Fumigacion f = Fumigacion.fromManualEntry(
                FECHA, PRODUCTO, DOSIS, UNIDAD,
                AREA, OBSERVACIONES, SIEMBRA_ID);

        assertNull(f.getAgriculturalInputId());
    }

    @Test
    void fromManualEntryDeberiaConservarElRestoDeLosCampos() {
        Fumigacion f = Fumigacion.fromManualEntry(
                FECHA, PRODUCTO, DOSIS, UNIDAD,
                AREA, OBSERVACIONES, SIEMBRA_ID);

        assertEquals(FECHA, f.getFecha());
        assertEquals(PRODUCTO, f.getProducto());
        assertEquals(DOSIS, f.getDosis());
        assertEquals(UNIDAD, f.getUnidadMedida());
        assertEquals(AREA, f.getAreaAplicada());
        assertEquals(OBSERVACIONES, f.getObservaciones());
        assertEquals(SIEMBRA_ID, f.getSiembraId());
    }

    // ── tieneInsumoDelCatalogo ───────────────────────────────────

    @Test
    void deberiaTenerInsumoDelCatalogoCuandoIdNoEsNulo() {
        Fumigacion f = Fumigacion.fromCatalog(
                FECHA, PRODUCTO, INSUMO_ID, DOSIS,
                UNIDAD, AREA, OBSERVACIONES, SIEMBRA_ID);

        assertTrue(f.tieneInsumoDelCatalogo());
    }

    @Test
    void noDeberiaTenerInsumoDelCatalogoCuandoIdEsNulo() {
        Fumigacion f = Fumigacion.fromManualEntry(
                FECHA, PRODUCTO, DOSIS, UNIDAD,
                AREA, OBSERVACIONES, SIEMBRA_ID);

        assertFalse(f.tieneInsumoDelCatalogo());
    }

    // ── Constructor vacío / setters ──────────────────────────────

    @Test
    void deberiaCrearFumigacionVaciaConConstructorPorDefecto() {
        Fumigacion f = new Fumigacion();

        assertNull(f.getId());
        assertNull(f.getFecha());
        assertNull(f.getProducto());
        assertNull(f.getAgriculturalInputId());
        assertNull(f.getDosis());
        assertNull(f.getUnidadMedida());
        assertNull(f.getAreaAplicada());
        assertNull(f.getObservaciones());
        assertNull(f.getSiembraId());
    }

    @Test
    void deberiaModificarCamposConSetters() {
        Fumigacion f = new Fumigacion();
        f.setId(1L);
        f.setFecha(FECHA);
        f.setProducto(PRODUCTO);
        f.setAgriculturalInputId(INSUMO_ID);
        f.setDosis(DOSIS);
        f.setUnidadMedida(UNIDAD);
        f.setAreaAplicada(AREA);
        f.setObservaciones(OBSERVACIONES);
        f.setSiembraId(SIEMBRA_ID);

        assertEquals(1L, f.getId());
        assertEquals(FECHA, f.getFecha());
        assertEquals(PRODUCTO, f.getProducto());
        assertEquals(INSUMO_ID, f.getAgriculturalInputId());
        assertEquals(DOSIS, f.getDosis());
        assertEquals(UNIDAD, f.getUnidadMedida());
        assertEquals(AREA, f.getAreaAplicada());
        assertEquals(OBSERVACIONES, f.getObservaciones());
        assertEquals(SIEMBRA_ID, f.getSiembraId());
    }

    // ── Unidades de medida ───────────────────────────────────────

    @Test
    void deberiaAceptarTodasLasUnidadesDeMedida() {
        for (UnidadMedida unidad : UnidadMedida.values()) {
            Fumigacion f = Fumigacion.fromCatalog(
                    FECHA, PRODUCTO, INSUMO_ID, DOSIS,
                    unidad, AREA, OBSERVACIONES, SIEMBRA_ID);

            assertEquals(unidad, f.getUnidadMedida());
        }
    }

    // ── Valores límite ───────────────────────────────────────────

    @Test
    void deberiaAceptarObservacionesNulas() {
        Fumigacion f = Fumigacion.fromCatalog(
                FECHA, PRODUCTO, INSUMO_ID, DOSIS,
                UNIDAD, AREA, null, SIEMBRA_ID);

        assertNull(f.getObservaciones());
        assertTrue(f.tieneInsumoDelCatalogo());
    }

    @Test
    void deberiaAceptarDosisCeroYAreaCero() {
        Fumigacion f = Fumigacion.fromManualEntry(
                FECHA, PRODUCTO, 0.0, UNIDAD, 0.0, OBSERVACIONES, SIEMBRA_ID);

        assertEquals(0.0, f.getDosis());
        assertEquals(0.0, f.getAreaAplicada());
    }
}