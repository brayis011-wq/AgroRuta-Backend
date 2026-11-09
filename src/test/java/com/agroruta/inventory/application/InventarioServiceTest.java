package com.agroruta.inventory.application;

import com.agroruta.inventory.application.ports.out.InventarioRepository;
import com.agroruta.agriculturalinput.domain.MeasurementUnit;
import com.agroruta.inventory.application.ports.in.InventarioUseCase.CostoEstimado;
import com.agroruta.inventory.application.ports.out.CatalogoInsumosPort;
import com.agroruta.inventory.domain.*;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("InventarioService")
class InventarioServiceTest {

    private final InventarioRepository repo = mock(InventarioRepository.class);
    private final CatalogoInsumosPort catalogo = mock(CatalogoInsumosPort.class);
    private final LocalDate hoy = LocalDate.of(2026, 11, 7);
    private final Long ana = 9L;
    private InventarioService service;

    private final InsumoCatalogo glifosato = new InsumoCatalogo(1L, "Glifosato", "HERBICIDA", MeasurementUnit.LITROS, true);
    private final InsumoCatalogo mancozeb = new InsumoCatalogo(2L, "Mancozeb", "FUNGICIDA", MeasurementUnit.GRAMOS, true);

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-07T15:00:00Z"), ZoneId.of("America/Bogota"));
        service = new InventarioService(repo, catalogo, reloj);
        when(catalogo.buscar(1L)).thenReturn(Optional.of(glifosato));
        when(catalogo.buscar(2L)).thenReturn(Optional.of(mancozeb));
        when(repo.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repo.minimosDe(ana)).thenReturn(Map.of());
    }

    private MovimientoInventario compra(Long insumo, double base, Double costo) {
        return MovimientoInventario.compra(ana, insumo, base, costo, hoy.minusDays(5), null, null);
    }

    @Test
    void laCompraSeGuardaEnLaUnidadBase() {
        MovimientoInventario m = service.registrarCompra(ana, 1L, 2.5, "LITROS", 90000.0, null, " Agro El Valle ", "");

        assertEquals(TipoMovimiento.COMPRA, m.getTipo());
        assertEquals(2500, m.getCantidad());
        assertEquals(90000.0, m.getCostoTotal());
        assertEquals(hoy, m.getFecha());
        assertEquals("Agro El Valle", m.getProveedor());
        assertNull(m.getObservaciones());
    }

    @Test
    void noSeCompraEnUnaUnidadDeOtroTipo() {
        assertThrows(BusinessException.class,
                () -> service.registrarCompra(ana, 1L, 2, "KG", null, null, null, null));
        verify(repo, never()).guardar(any());
    }

    @Test
    void noSeCompraConFechaFutura() {
        assertThrows(BusinessException.class,
                () -> service.registrarCompra(ana, 1L, 1, "LITROS", null, hoy.plusDays(1), null, null));
    }

    @Test
    void insumoInexistenteDa404() {
        when(catalogo.buscar(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.registrarCompra(ana, 99L, 1, "LITROS", null, null, null, null));
    }

    @Test
    void elAjusteRegistraLaDiferenciaConElConteo() {
        when(repo.movimientosDe(ana, 2L)).thenReturn(List.of(compra(2L, 3000, null)));

        MovimientoInventario m = service.ajustarA(ana, 2L, 2.2, "KG", "conteo de fin de mes");

        assertEquals(TipoMovimiento.AJUSTE, m.getTipo());
        assertEquals(-800, m.getCantidad(), 0.0001);
    }

    @Test
    void unAjusteSinDiferenciaSeRechaza() {
        when(repo.movimientosDe(ana, 2L)).thenReturn(List.of(compra(2L, 3000, null)));
        assertThrows(BusinessException.class, () -> service.ajustarA(ana, 2L, 3000, "GRAMOS", null));
    }

    @Test
    void elGastoDeUnaFumigacionNoSeBorraDesdeElInventario() {
        MovimientoInventario uso = MovimientoInventario.uso(ana, 1L, 500, hoy, 7L);
        uso.setId(40L);
        when(repo.buscarMovimiento(40L)).thenReturn(Optional.of(uso));

        assertThrows(BusinessException.class, () -> service.eliminarMovimiento(ana, 40L));
        verify(repo, never()).eliminarMovimiento(any());
    }

    @Test
    void nadieBorraMovimientosDeOtraBodega() {
        MovimientoInventario ajena = compra(1L, 1000, null);
        ajena.setAgricultorId(55L);
        when(repo.buscarMovimiento(41L)).thenReturn(Optional.of(ajena));

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarMovimiento(ana, 41L));
    }

    @Test
    void registrarUsoDescuentaYDevuelveElCostoAPrecioPromedio() {
        when(repo.usoDeFumigacion(7L)).thenReturn(Optional.empty());
        when(repo.movimientosDe(ana, 1L)).thenReturn(List.of(compra(1L, 4000, 120000.0)));  // 30 $/ml

        Optional<Double> costo = service.registrarUso(7L, ana, 1L, 0.75, "LITROS", hoy);

        ArgumentCaptor<MovimientoInventario> g = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(repo).guardar(g.capture());
        assertEquals(TipoMovimiento.USO, g.getValue().getTipo());
        assertEquals(750, g.getValue().getCantidad());
        assertEquals(7L, g.getValue().getFumigacionId());
        assertEquals(Optional.of(22500.0), costo);
    }

    @Test
    void registrarUsoOtraVezCorrigeElMismoMovimiento() {
        MovimientoInventario anterior = MovimientoInventario.uso(ana, 1L, 500, hoy.minusDays(1), 7L);
        anterior.setId(70L);
        when(repo.usoDeFumigacion(7L)).thenReturn(Optional.of(anterior));
        when(repo.movimientosDe(ana, 1L)).thenReturn(List.of());

        Optional<Double> costo = service.registrarUso(7L, ana, 1L, 300, "ML", hoy);

        assertEquals(70L, anterior.getId());
        assertEquals(300, anterior.getCantidad());
        assertEquals(hoy, anterior.getFecha());
        assertTrue(costo.isEmpty(), "sin compras con precio no hay costo");
    }

    @Test
    void quitarUsoDevuelveElProductoALaBodega() {
        MovimientoInventario uso = MovimientoInventario.uso(ana, 1L, 500, hoy, 7L);
        uso.setId(70L);
        when(repo.usoDeFumigacion(7L)).thenReturn(Optional.of(uso));

        service.quitarUso(7L);

        verify(repo).eliminarMovimiento(70L);
    }

    @Test
    void elStockIncluyeLosInsumosConMinimoAunqueNoTenganMovimientosYVaPorNombre() {
        when(repo.movimientosDe(ana)).thenReturn(List.of(compra(2L, 1000, null)));
        when(repo.minimosDe(ana)).thenReturn(Map.of(1L, 500.0));

        List<StockInsumo> stock = service.stock(ana);

        assertEquals(List.of("Glifosato", "Mancozeb"), stock.stream().map(StockInsumo::nombre).toList());
        assertEquals(EstadoStock.AGOTADO, stock.get(0).estado());
        assertEquals(EstadoStock.OK, stock.get(1).estado());
    }

    @Test
    void losAvisosSoloTraenLoQueNecesitaAtencion() {
        when(repo.movimientosDe(ana)).thenReturn(List.of(compra(1L, 400, null), compra(2L, 5000, null)));
        when(repo.minimosDe(ana)).thenReturn(Map.of(1L, 500.0, 2L, 1000.0));

        List<StockInsumo> avisos = service.avisos(ana);

        assertEquals(1, avisos.size());
        assertEquals("Glifosato", avisos.get(0).nombre());
        assertEquals(EstadoStock.BAJO, avisos.get(0).estado());
    }

    @Test
    void elMinimoSeGuardaEnUnidadBase() {
        when(repo.movimientosDe(ana, 2L)).thenReturn(List.of(compra(2L, 800, null)));

        StockInsumo s = service.definirMinimo(ana, 2L, 1.0, "KG");

        verify(repo).guardarMinimo(ana, 2L, 1000.0);
        assertEquals(EstadoStock.BAJO, s.estado());
    }

    @Test
    void costoEstimadoConPrecioPromedio() {
        when(repo.movimientosDe(ana, 2L)).thenReturn(List.of(compra(2L, 2000, 50000.0)));  // 25 $/g

        CostoEstimado c = service.costoEstimado(ana, 2L, 0.4, "KG");

        assertEquals(10000.0, c.costo());
        assertEquals(2000, c.disponible());
        assertEquals(UnidadBase.GRAMOS, c.unidadBase());
    }
}
