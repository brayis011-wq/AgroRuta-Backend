package com.agroruta.worker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.EstadoInvalidoException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.application.ports.out.CultivoPort;
import com.agroruta.worker.application.ports.out.JornalRepository;
import com.agroruta.worker.application.ports.out.LaborRepository;
import com.agroruta.worker.application.ports.out.NominaRepository;
import com.agroruta.worker.application.ports.out.PagoRepository;
import com.agroruta.worker.application.ports.out.TrabajadorRepository;
import com.agroruta.worker.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("Planilla semanal, nóminas del equipo y pagos juntos")
class PlanillaYLotesTest {

    private static final Long AGRICULTOR = 7L;
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 16);

    @Mock
    JornalRepository jornales;

    @Mock
    TrabajadorRepository trabajadores;

    @Mock
    LaborRepository labores;

    @Mock
    CultivoPort cultivos;

    @Mock
    NominaRepository nominas;

    @Mock
    PagoRepository pagos;

    @Mock
    ApplicationEventPublisher eventos;

    JornalService jornalService;
    NominaService nominaService;
    PagoService pagoService;
    Trabajador ana;
    Trabajador beto;

    @BeforeEach
    void setUp() {
        jornalService = new JornalService(jornales, trabajadores, labores, cultivos);
        nominaService = new NominaService(nominas, jornales, trabajadores);
        pagoService = new PagoService(pagos, nominas, jornales, eventos);
        ana = trabajador(1L, "Ana");
        beto = trabajador(2L, "Beto");
    }

    private static Trabajador trabajador(Long id, String nombre) {
        Trabajador t = new Trabajador(
                id,
                nombre,
                "López",
                "c" + id,
                null,
                null,
                LUNES.minusYears(1),
                TipoContrato.JORNAL,
                new Cargo(1L, "Oficios", null, new BigDecimal("60000"), true));
        t.setAgricultorId(AGRICULTOR);
        return t;
    }

    private static Jornal jornal(Long id, Trabajador t, LocalDate fecha) {
        return new Jornal(id, fecha, t, 5L, "Lote 1", null);
    }

    @Test
    void laPlanillaJuntaLosActivosYMarcaLoQueYaEstaEnNomina() {
        when(jornales.buscarDelEquipo(AGRICULTOR, LUNES, LUNES.plusDays(6)))
                .thenReturn(List.of(jornal(10L, beto, LUNES), jornal(11L, beto, LUNES.plusDays(1))));
        when(trabajadores.listarActivosPorAgricultor(AGRICULTOR)).thenReturn(List.of(beto, ana));
        when(jornales.estadosDeNomina(anyCollection())).thenReturn(java.util.Map.of(10L, EstadoNomina.APROBADA));

        PlanillaSemanal p = jornalService.planilla(AGRICULTOR, LUNES, LUNES.plusDays(6));

        assertThat(p.trabajadores()).extracting(Trabajador::getNombre).containsExactly("Ana", "Beto");
        assertThat(p.jornales()).hasSize(2);
        assertThat(p.enNomina()).containsOnlyKeys(10L);
    }

    @Test
    void laPlanillaNoPasaDeDosSemanas() {
        assertThatThrownBy(() -> jornalService.planilla(AGRICULTOR, LUNES, LUNES.plusDays(30)))
                .isInstanceOf(com.agroruta.shared.exception.DatoInvalidoException.class);
    }

    @Test
    void anotarVariosNoRepiteNiAceptaTrabajadoresAjenos() {
        when(trabajadores.buscarPorId(1L)).thenReturn(Optional.of(ana));
        when(jornales.buscarPorTrabajadorYPeriodo(1L, LUNES, LUNES)).thenReturn(List.of(jornal(1L, ana, LUNES)));
        var yaEstaba = jornalService.registrarVarios(
                AGRICULTOR,
                5L,
                List.of(3L),
                List.of(new PlanillaSemanal.Entrada(1L, LUNES, null), new PlanillaSemanal.Entrada(1L, LUNES, null)));
        assertThat(yaEstaba).isEmpty();
        verify(jornales, never()).guardar(any());

        Trabajador ajeno = trabajador(3L, "Otro");
        ajeno.setAgricultorId(99L);
        when(trabajadores.buscarPorId(3L)).thenReturn(Optional.of(ajeno));
        assertThatThrownBy(() -> jornalService.registrarVarios(
                        AGRICULTOR, 5L, List.of(3L), List.of(new PlanillaSemanal.Entrada(3L, LUNES, null))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void unJornalEnNominaAprobadaNoSePuedeQuitar() {
        when(jornales.buscarPorId(10L)).thenReturn(Optional.of(jornal(10L, ana, LUNES)));
        when(jornales.estadosDeNomina(List.of(10L))).thenReturn(java.util.Map.of(10L, EstadoNomina.APROBADA));
        assertThatThrownBy(() -> jornalService.eliminarJornal(10L)).isInstanceOf(EstadoInvalidoException.class);
        verify(jornales, never()).eliminar(any());

        when(jornales.buscarPorId(11L)).thenReturn(Optional.of(jornal(11L, ana, LUNES)));
        when(jornales.estadosDeNomina(List.of(11L))).thenReturn(java.util.Map.of());
        jornalService.eliminarJornal(11L);
        verify(jornales).eliminar(11L);
    }

    @Test
    void quitarUnJornalDeLaPlanillaCorrigeSuNominaPendiente() {
        jornalService.setNominaRepository(nominas);
        Jornal a = jornal(12L, ana, LUNES), b = jornal(13L, ana, LUNES.plusDays(1));
        Nomina n = new Nomina(30L, ana, LUNES, LUNES.plusDays(14), new ArrayList<>(List.of(a, b)));
        when(jornales.buscarPorId(12L)).thenReturn(Optional.of(a));
        when(jornales.estadosDeNomina(List.of(12L))).thenReturn(java.util.Map.of(12L, EstadoNomina.PENDIENTE));
        when(nominas.buscarPorJornal(12L)).thenReturn(Optional.of(n));

        jornalService.eliminarJornal(12L);

        assertThat(n.getJornales()).extracting(Jornal::getId).containsExactly(13L);
        assertThat(n.getValorTotal()).isEqualByComparingTo("60000");
        verify(nominas).guardar(n);
        verify(jornales).eliminar(12L);
    }

    @Test
    void siEraElUnicoJornalSeEliminaLaNominaPendiente() {
        jornalService.setNominaRepository(nominas);
        Jornal a = jornal(14L, ana, LUNES);
        when(jornales.buscarPorId(14L)).thenReturn(Optional.of(a));
        when(jornales.estadosDeNomina(List.of(14L))).thenReturn(java.util.Map.of(14L, EstadoNomina.PENDIENTE));
        when(nominas.buscarPorJornal(14L))
                .thenReturn(Optional.of(new Nomina(31L, ana, LUNES, LUNES, new ArrayList<>(List.of(a)))));
        jornalService.eliminarJornal(14L);
        verify(nominas).eliminar(31L);
        verify(jornales).eliminar(14L);
    }

    @Test
    void unJornalNuevoEntraEnLaNominaPendienteQueCubreEseDia() {
        jornalService.setNominaRepository(nominas);
        Nomina n = new Nomina(32L, ana, LUNES, LUNES.plusDays(14), new ArrayList<>(List.of(jornal(15L, ana, LUNES))));
        when(trabajadores.buscarPorId(1L)).thenReturn(Optional.of(ana));
        when(jornales.buscarPorTrabajadorYPeriodo(any(), any(), any())).thenReturn(List.of());
        when(cultivos.buscar(5L))
                .thenReturn(Optional.of(new CultivoPort.CultivoInfo(
                        5L, AGRICULTOR, 7L, "La Esperanza", 2L, "Lote 1", "Café", "Castillo", "COSECHA")));
        when(labores.buscarPorId(3L)).thenReturn(Optional.of(new Labor(3L, "Recolección", null)));
        when(jornales.guardar(any())).thenAnswer(i -> {
            Jornal j = i.getArgument(0);
            j.setId(16L);
            return j;
        });
        when(nominas.buscarPorTrabajadorYEstado(1L, EstadoNomina.PENDIENTE)).thenReturn(List.of(n));

        jornalService.registrarVarios(
                AGRICULTOR, 5L, List.of(3L), List.of(new PlanillaSemanal.Entrada(1L, LUNES.plusDays(2), null)));

        assertThat(n.getJornales()).hasSize(2);
        assertThat(n.getValorTotal()).isEqualByComparingTo("120000");
        verify(nominas).guardar(n);
    }

    @Test
    void unaNominaAprobadaNoSeCorrige() {
        Nomina n = new Nomina(
                33L, ana, LUNES, LUNES, new ArrayList<>(List.of(jornal(17L, ana, LUNES), jornal(18L, ana, LUNES))));
        n.aprobar();
        assertThatThrownBy(() -> n.quitarJornal(17L)).isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    void laQuincenaDelEquipoSaltaAQuienNoTieneJornales() {
        LocalDate fin = LUNES.plusDays(14);
        when(trabajadores.listarTodosPorAgricultor(AGRICULTOR)).thenReturn(List.of(ana, beto));
        when(jornales.buscarDisponiblesParaNomina(1L, LUNES, fin))
                .thenReturn(new ArrayList<>(List.of(jornal(1L, ana, LUNES))));
        when(jornales.buscarDisponiblesParaNomina(2L, LUNES, fin)).thenReturn(List.of());
        when(nominas.guardar(any())).thenAnswer(i -> i.getArgument(0));

        List<Nomina> creadas = nominaService.generarParaTodos(AGRICULTOR, LUNES, fin);

        assertThat(creadas).singleElement().satisfies(n -> {
            assertThat(n.getTrabajador().getNombre()).isEqualTo("Ana");
            assertThat(n.getValorTotal()).isEqualByComparingTo("60000");
        });
    }

    @Test
    void sinJornalesEnElEquipoAvisa() {
        when(trabajadores.listarTodosPorAgricultor(AGRICULTOR)).thenReturn(List.of(ana));
        when(jornales.buscarDisponiblesParaNomina(any(), any(), any())).thenReturn(List.of());
        assertThatThrownBy(() -> nominaService.generarParaTodos(AGRICULTOR, LUNES, LUNES.plusDays(14)))
                .isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    void pagarVariasApruebaLasPendientesYPagaCadaUnaPorSuTotal() {
        Nomina n = new Nomina(20L, ana, LUNES, LUNES.plusDays(14), new ArrayList<>(List.of(jornal(1L, ana, LUNES))));
        when(nominas.buscarPorId(20L)).thenReturn(Optional.of(n));
        when(nominas.guardar(any())).thenAnswer(i -> i.getArgument(0));
        when(pagos.buscarPorNomina(20L)).thenReturn(Optional.empty());
        when(pagos.guardar(any())).thenAnswer(i -> {
            Pago p = i.getArgument(0);
            p.setId(30L);
            return p;
        });

        List<Pago> hechos = pagoService.pagarVarias(AGRICULTOR, List.of(20L, 20L), MetodoPago.EFECTIVO, null, true);

        assertThat(hechos)
                .singleElement()
                .satisfies(p -> assertThat(p.getMonto()).isEqualByComparingTo("60000"));
        assertThat(n.getEstado()).isEqualTo(EstadoNomina.PAGADA);
        verify(eventos).publishEvent(any(com.agroruta.shared.evento.PagoRegistrado.class));
    }

    @Test
    void noSePuedenPagarNominasDeOtroAgricultor() {
        Trabajador ajeno = trabajador(3L, "Otro");
        ajeno.setAgricultorId(99L);
        when(nominas.buscarPorId(21L)).thenReturn(Optional.of(new Nomina(21L, ajeno, LUNES, LUNES, new ArrayList<>())));
        assertThatThrownBy(() -> pagoService.pagarVarias(AGRICULTOR, List.of(21L), MetodoPago.EFECTIVO, null, true))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(pagos, never()).guardar(any());
    }
}
