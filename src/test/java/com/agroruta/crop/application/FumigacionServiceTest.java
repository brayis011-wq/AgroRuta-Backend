package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.out.FincaRepository;
import com.agroruta.crop.application.ports.out.FumigacionRepository;
import com.agroruta.crop.application.ports.out.LoteRepository;
import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.application.ports.out.InventarioPort;
import com.agroruta.crop.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FumigacionServiceTest {

    @Mock
    private FumigacionRepository fumigacionRepository;

    @Mock
    private SiembraUseCase siembraUseCase;

    @Mock
    private InventarioPort inventario;

    @Mock
    private LoteRepository loteRepository;

    @Mock
    private FincaRepository fincaRepository;

    @InjectMocks
    private FumigacionService service;

    private final Long siembraId = 1L;
    private final LocalDate fecha = LocalDate.of(2026, 3, 10);

    @BeforeEach
    void setUp() {
        Siembra siembra = new Siembra(siembraId, fecha, 100, Variedades.COLOMBIA, 1L);
        lenient().when(siembraUseCase.buscarSiembraPorId(siembraId)).thenReturn(siembra);
    }

    @Test
    void deberiaRegistrarFumigacionDesdeCatalogo() {
        Fumigacion fumigacion = Fumigacion.fromCatalog(
                fecha, "Fungicida X", 10L, 2.5, UnidadMedida.LITROS, 5.0, "obs", siembraId);
        when(fumigacionRepository.save(any())).thenReturn(fumigacion);

        Fumigacion resultado = service.registrarFumigacion(
                fecha, "Fungicida X", 10L, 2.5, "LITROS", 5.0, "obs", siembraId);

        assertNotNull(resultado);
        assertTrue(resultado.tieneInsumoDelCatalogo());
        assertEquals("Fungicida X", resultado.getProducto());
        verify(fumigacionRepository).save(any());
    }

    @Test
    void deberiaRegistrarFumigacionManual() {
        Fumigacion fumigacion = Fumigacion.fromManualEntry(
                fecha, "Producto manual", 1.0, UnidadMedida.KG, 3.0, "obs", siembraId);
        when(fumigacionRepository.save(any())).thenReturn(fumigacion);

        Fumigacion resultado = service.registrarFumigacion(
                fecha, "Producto manual", null, 1.0, "KG", 3.0, "obs", siembraId);

        assertNotNull(resultado);
        assertFalse(resultado.tieneInsumoDelCatalogo());
        verify(fumigacionRepository).save(any());
    }

    @Test
    void deberiaLanzarExcepcionSiUnidadMedidaEsInvalida() {
        assertThrows(IllegalArgumentException.class, () ->
                service.registrarFumigacion(
                        fecha, "Producto", null, 1.0, "INVALIDA", 3.0, "obs", siembraId));
    }

    @Test
    void deberiaListarFumigacionesPorSiembra() {
        List<Fumigacion> fumigaciones = List.of(
                Fumigacion.fromManualEntry(fecha, "Prod A", 1.0, UnidadMedida.LITROS, 2.0, "obs", siembraId),
                Fumigacion.fromManualEntry(fecha, "Prod B", 2.0, UnidadMedida.ML, 3.0, "obs", siembraId)
        );
        when(fumigacionRepository.findBySiembraId(siembraId)).thenReturn(fumigaciones);

        List<Fumigacion> resultado = service.listarFumigacionesPorSiembra(siembraId);

        assertEquals(2, resultado.size());
        verify(fumigacionRepository).findBySiembraId(siembraId);
    }

    // ── Bodega de insumos ─────────────────────────────────────────────

    private void duenoDeLaSiembra(Long agricultorId) {
        when(loteRepository.findById(1L)).thenReturn(java.util.Optional.of(new Lote(1L, "Lote A", 2.0, 3L)));
        when(fincaRepository.findById(3L)).thenReturn(java.util.Optional.of(
                new Finca(3L, "El Triunfo", "Pasca", 10.0, agricultorId)));
    }

    @Test
    void conCantidadUsadaDescuentaDeLaBodegaYCalculaElCostoSiNoSeEscribio() {
        duenoDeLaSiembra(9L);
        when(fumigacionRepository.save(any())).thenAnswer(inv -> {
            Fumigacion f = inv.getArgument(0);
            if (f.getId() == null) f.setId(70L);
            return f;
        });
        when(inventario.registrarUso(70L, 9L, 10L, 0.75, "LITROS", fecha)).thenReturn(java.util.Optional.of(22500.0));

        Fumigacion f = service.registrarFumigacion(fecha, "Glifosato", 10L, 2.5, "LITROS", 5.0, null, siembraId,
                new RestriccionesFumigacion(null, null, null, 0.75, "litros"));

        verify(inventario).registrarUso(70L, 9L, 10L, 0.75, "LITROS", fecha);
        assertEquals(22500.0, f.getCosto());
        assertEquals(UnidadMedida.LITROS, f.getUnidadCantidad());
    }

    @Test
    void elCostoEscritoAManoNoSeReemplaza() {
        duenoDeLaSiembra(9L);
        when(fumigacionRepository.save(any())).thenAnswer(inv -> {
            Fumigacion f = inv.getArgument(0);
            f.setId(70L);
            return f;
        });
        when(inventario.registrarUso(any(), any(), any(), anyDouble(), any(), any()))
                .thenReturn(java.util.Optional.of(22500.0));

        Fumigacion f = service.registrarFumigacion(fecha, "Glifosato", 10L, 2.5, "LITROS", 5.0, null, siembraId,
                new RestriccionesFumigacion(null, null, 18000.0, 0.75, "LITROS"));

        assertEquals(18000.0, f.getCosto());
        verify(fumigacionRepository, times(1)).save(any());
    }

    @Test
    void unProductoEscritoAManoNoTocaLaBodega() {
        Fumigacion manual = Fumigacion.fromManualEntry(fecha, "Caldo bordelés", 1.0, UnidadMedida.KG, 3.0, null, siembraId);
        manual.setId(71L);
        when(fumigacionRepository.save(any())).thenReturn(manual);

        service.registrarFumigacion(fecha, "Caldo bordelés", null, 1.0, "KG", 3.0, null, siembraId,
                new RestriccionesFumigacion(null, null, null, 2.0, "KG"));

        verify(inventario, never()).registrarUso(any(), any(), any(), anyDouble(), any(), any());
        verify(inventario).quitarUso(71L);
    }

    @Test
    void alQuitarLaCantidadEnUnaEdicionElProductoVuelveALaBodega() {
        Fumigacion guardada = Fumigacion.fromCatalog(fecha, "Glifosato", 10L, 2.5, UnidadMedida.LITROS, 5.0, null, siembraId);
        guardada.setId(70L);
        guardada.fijarRestricciones(new RestriccionesFumigacion(null, null, 22500.0, 0.75, "LITROS"));
        when(fumigacionRepository.findById(70L)).thenReturn(java.util.Optional.of(guardada));
        when(fumigacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Fumigacion f = service.actualizarFumigacion(70L, fecha, "Glifosato", 10L, 2.5, "LITROS", 5.0, null,
                new RestriccionesFumigacion(null, null, 22500.0, null, null));

        assertNull(f.getCantidadUsada());
        verify(inventario).quitarUso(70L);
        verify(inventario, never()).registrarUso(any(), any(), any(), anyDouble(), any(), any());
    }

    @Test
    void alBorrarLaFumigacionSuProductoVuelveALaBodega() {
        Fumigacion f = Fumigacion.fromCatalog(fecha, "Glifosato", 10L, 2.5, UnidadMedida.LITROS, 5.0, null, siembraId);
        f.setId(70L);
        when(fumigacionRepository.findById(70L)).thenReturn(java.util.Optional.of(f));

        service.eliminarFumigacion(70L);

        var orden = inOrder(inventario, fumigacionRepository);
        orden.verify(inventario).quitarUso(70L);
        orden.verify(fumigacionRepository).deleteById(70L);
    }

    @Test
    void unaUnidadDeCantidadInvalidaSeRechaza() {
        assertThrows(RuntimeException.class, () -> service.registrarFumigacion(fecha, "Glifosato", 10L, 2.5,
                "LITROS", 5.0, null, siembraId, new RestriccionesFumigacion(null, null, null, 1.0, "BARRILES")));
        verify(fumigacionRepository, never()).save(any());
    }
}
