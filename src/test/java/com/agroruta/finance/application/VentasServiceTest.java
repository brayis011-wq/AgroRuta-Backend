package com.agroruta.finance.application;

import com.agroruta.finance.application.ports.in.VentasUseCase.DatosVenta;
import com.agroruta.finance.application.ports.out.VentasRepository;
import com.agroruta.finance.domain.KilosEnBodega;
import com.agroruta.finance.domain.ResumenVentas;
import com.agroruta.finance.domain.Venta;
import com.agroruta.shared.exception.DatoInvalidoException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.shared.util.ZonaHoraria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Ventas y cuentas por cobrar (servicio)")
class VentasServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 25);
    private static final Long AGRICULTOR = 7L;
    private static final Long SIEMBRA = 34L;

    private VentasRepository repo;
    private VentasService servicio;

    @BeforeEach
    void preparar() {
        repo = mock(VentasRepository.class);
        Clock reloj = Clock.fixed(HOY.atTime(10, 0).atZone(ZonaHoraria.COLOMBIA).toInstant(), ZonaHoraria.COLOMBIA);
        servicio = new VentasService(repo, reloj);
    }

    private static DatosVenta datos(Long siembraId, String kg, String precio) {
        return new DatosVenta(siembraId, "Cooperativa", HOY, "Café pergamino",
                kg == null ? null : new BigDecimal(kg), precio == null ? null : new BigDecimal(precio),
                null, null, null);
    }

    private static KilosEnBodega bodega(String cosechados, String vendidos) {
        return new KilosEnBodega(SIEMBRA, "Café", "Castillo", "Lote 1", "La Esperanza",
                new BigDecimal(cosechados), new BigDecimal(vendidos), HOY.minusDays(3), false);
    }

    private static Venta venta(Long id, String comprador, LocalDate fecha, String total, String abonado, LocalDate vence) {
        return new Venta(id, null, null, comprador, fecha, "Mora", null, null, new BigDecimal(total), vence, null,
                new BigDecimal(abonado), List.of());
    }

    /** El repositorio devuelve lo que se creó, con id 1 */
    private void crearDevuelveId() {
        when(repo.crear(eq(AGRICULTOR), any())).thenAnswer(inv -> {
            Venta v = inv.getArgument(1);
            return new Venta(1L, v.siembraId(), null, v.comprador(), v.fecha(), v.producto(), v.cantidadKg(),
                    v.precioKg(), v.total(), v.vence(), v.observaciones(), BigDecimal.ZERO, List.of());
        });
        when(repo.buscarPorId(1L)).thenAnswer(inv -> Optional.of(
                new Venta(1L, SIEMBRA, null, "Cooperativa", HOY, "Café pergamino", new BigDecimal("100"),
                        new BigDecimal("17800"), new BigDecimal("1780000"), null, null, BigDecimal.ZERO, List.of())));
    }

    @Nested
    @DisplayName("Kilos de la siembra")
    class Kilos {

        @Test
        void noSePuedeVenderMasDeLoQueQueda() {
            when(repo.kilosDeSiembra(SIEMBRA)).thenReturn(Optional.of(bodega("300", "250")));

            assertThatThrownBy(() -> servicio.registrar(AGRICULTOR, datos(SIEMBRA, "60", "17800"), false, null))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessageContaining("50");
            verify(repo, never()).crear(any(), any());
        }

        @Test
        void conLosKilosQueQuedanSeRegistra() {
            when(repo.kilosDeSiembra(SIEMBRA)).thenReturn(Optional.of(bodega("300", "200")));
            crearDevuelveId();

            Venta v = servicio.registrar(AGRICULTOR, datos(SIEMBRA, "100", "17800"), false, null);

            assertThat(v.id()).isEqualTo(1L);
            verify(repo, never()).agregarAbono(any());
        }

        @Test
        void unaSiembraSinCosechasNoSeVende() {
            when(repo.kilosDeSiembra(SIEMBRA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.registrar(AGRICULTOR, datos(SIEMBRA, "10", "17800"), false, null))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessageContaining("registra la cosecha");
        }

        @Test
        void vendiendoDeUnaSiembraHayQueDecirLosKilos() {
            assertThatThrownBy(() -> servicio.registrar(AGRICULTOR, datos(SIEMBRA, null, null), false, null))
                    .isInstanceOf(DatoInvalidoException.class);
        }

        @Test
        void alEditarBajarLosKilosSiempreSePuede() {
            Venta antes = new Venta(5L, SIEMBRA, null, "Cooperativa", HOY, "Café", new BigDecimal("120"),
                    new BigDecimal("17800"), new BigDecimal("2136000"), null, null, BigDecimal.ZERO, List.of());
            when(repo.buscarPorId(5L)).thenReturn(Optional.of(antes));

            servicio.actualizar(5L, datos(SIEMBRA, "100", "17800"));

            verify(repo, never()).kilosDeSiembra(any());
            verify(repo).actualizar(any());
        }
    }

    @Nested
    @DisplayName("Pagos")
    class Pagos {

        @Test
        void deContadoQuedaUnAbonoPorElTotal() {
            when(repo.kilosDeSiembra(SIEMBRA)).thenReturn(Optional.of(bodega("300", "0")));
            crearDevuelveId();

            servicio.registrar(AGRICULTOR, datos(SIEMBRA, "100", "17800"), true, "transferencia");

            ArgumentCaptor<Venta.Abono> abono = ArgumentCaptor.forClass(Venta.Abono.class);
            verify(repo).agregarAbono(abono.capture());
            assertThat(abono.getValue().monto()).isEqualByComparingTo("1780000");
            assertThat(abono.getValue().metodo()).isEqualTo("TRANSFERENCIA");
        }

        @Test
        void unPagoNoPuedePasarDeLoQueDeben() {
            when(repo.buscarPorId(9L)).thenReturn(Optional.of(venta(9L, "Juan", HOY.minusDays(10), "500000", "400000", null)));

            assertThatThrownBy(() -> servicio.abonar(9L, HOY, new BigDecimal("150000"), "EFECTIVO", null))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessageContaining("mayor que lo que te deben");
        }

        @Test
        void unPagoNoPuedeSerAnteriorALaVenta() {
            when(repo.buscarPorId(9L)).thenReturn(Optional.of(venta(9L, "Juan", HOY, "500000", "0", null)));

            assertThatThrownBy(() -> servicio.abonar(9L, HOY.minusDays(1), BigDecimal.TEN, "EFECTIVO", null))
                    .isInstanceOf(DatoInvalidoException.class);
        }

        @Test
        void unMetodoDePagoDesconocidoNoSeAcepta() {
            when(repo.buscarPorId(9L)).thenReturn(Optional.of(venta(9L, "Juan", HOY, "500000", "0", null)));

            assertThatThrownBy(() -> servicio.abonar(9L, HOY, BigDecimal.TEN, "BITCOIN", null))
                    .isInstanceOf(DatoInvalidoException.class);
        }

        @Test
        void elTotalNoPuedeQuedarPorDebajoDeLoAbonado() {
            when(repo.buscarPorId(9L)).thenReturn(Optional.of(venta(9L, "Juan", HOY, "500000", "300000", null)));
            DatosVenta menos = new DatosVenta(null, "Juan", HOY, "Mora", null, null, new BigDecimal("200000"), null, null);

            assertThatThrownBy(() -> servicio.actualizar(9L, menos))
                    .isInstanceOf(DatoInvalidoException.class)
                    .hasMessageContaining("abonaron");
        }

        @Test
        void quitarUnPagoQueNoEsDeLaVentaEsNoEncontrado() {
            when(repo.buscarPorId(9L)).thenReturn(Optional.of(venta(9L, "Juan", HOY, "500000", "0", null)));

            assertThatThrownBy(() -> servicio.quitarAbono(9L, 77L)).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Resumen y filtros")
    class Resumen {

        @Test
        void sumaLoQueDebenYAgrupaPorComprador() {
            when(repo.listar(AGRICULTOR, null, null)).thenReturn(List.of(
                    venta(1L, "Juan ", HOY.minusDays(30), "500000", "100000", HOY.minusDays(2)), // vencida: 400.000
                    venta(2L, "juan", HOY.minusDays(1), "300000", "0", HOY.plusDays(10)),       // pendiente: 300.000
                    venta(3L, "Cooperativa", HOY.minusDays(5), "1000000", "1000000", null)));    // pagada
            when(repo.cobradoEntre(eq(AGRICULTOR), any(), any())).thenReturn(null);

            ResumenVentas r = servicio.resumen(AGRICULTOR);

            assertThat(r.porCobrar()).isEqualByComparingTo("700000");
            assertThat(r.vencido()).isEqualByComparingTo("400000");
            assertThat(r.ventasVencidas()).isEqualTo(1);
            assertThat(r.ventasPendientes()).isEqualTo(1);
            assertThat(r.vendidoMes()).isEqualByComparingTo("1300000"); // la de hace 30 días es del mes pasado
            assertThat(r.cobradoMes()).isEqualByComparingTo("0");
            assertThat(r.compradores()).singleElement().satisfies(d -> {
                assertThat(d.comprador()).isEqualTo("Juan");
                assertThat(d.saldo()).isEqualByComparingTo("700000");
                assertThat(d.ventas()).isEqualTo(2);
                assertThat(d.conVencidas()).isTrue();
            });
        }

        @Test
        void filtraPorEstadoYCompradorSinImportarMayusculas() {
            when(repo.listar(AGRICULTOR, null, null)).thenReturn(List.of(
                    venta(1L, "Juan Pérez", HOY.minusDays(30), "500000", "0", HOY.minusDays(2)),
                    venta(2L, "Cooperativa", HOY.minusDays(1), "300000", "0", null)));

            assertThat(servicio.listar(AGRICULTOR, Venta.Estado.VENCIDA, null, null, null))
                    .extracting(Venta::id).containsExactly(1L);
            assertThat(servicio.listar(AGRICULTOR, null, " COOP", null, null))
                    .extracting(Venta::id).containsExactly(2L);
        }
    }
}
