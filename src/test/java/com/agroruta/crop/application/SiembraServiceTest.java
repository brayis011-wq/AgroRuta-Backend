package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.in.LoteUseCase;
import com.agroruta.crop.domain.*;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SiembraServiceTest {

    @Mock private SiembraRepository siembraRepository;
    @Mock private LoteUseCase loteUseCase;
    @Mock private CatalogoCultivoRepository catalogo;
    @Mock private EtapaSiembraRepository etapaRepository;
    @Mock private CosechaRepository cosechaRepository;
    @Mock private FumigacionRepository fumigacionRepository;
    @Mock private ActividadCultivoRepository actividadRepository;

    @InjectMocks
    private SiembraService service;

    private final Long loteId = 1L;
    private final LocalDate fecha = LocalDate.of(2026, 1, 10);

    @BeforeEach
    void setUp() {
        Lote lote = new Lote(loteId, "Lote A", 10.0, 1L);
        when(loteUseCase.buscarLotePorId(loteId)).thenReturn(lote);
        when(catalogo.buscarVariedad(1L)).thenReturn(Optional.of(Variedades.COLOMBIA));
        when(siembraRepository.findActivaByLoteId(any())).thenReturn(Optional.empty());
        when(siembraRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(etapaRepository.findUltima(any())).thenReturn(Optional.empty());
    }

    private Siembra siembraEn(EstadoCultivo estado) {
        Siembra s = new Siembra(1L, fecha, 500, Variedades.COLOMBIA, loteId);
        s.setEstadoCultivo(estado);
        when(siembraRepository.findById(1L)).thenReturn(Optional.of(s));
        return s;
    }

    // ── registrar ─────────────────────────────────────────────────────

    @Test
    void deberiaGuardarCultivoYVariedadDelCatalogo() {
        Siembra resultado = service.registrarSiembra(fecha, 500, 1L, loteId);

        assertEquals("Uchuva", resultado.getTipoCultivo());
        assertEquals("Colombia", resultado.getVariedad());
        assertEquals(1L, resultado.getVariedadId());
        assertEquals("Uchuva · Colombia", resultado.nombreCompleto());
    }

    @Test
    void alSembrarElLoteQuedaEnCultivoYSeRegistraLaGerminacion() {
        when(siembraRepository.save(any())).thenAnswer(inv -> {
            Siembra s = inv.getArgument(0);
            s.setId(7L);
            return s;
        });

        service.registrarSiembra(fecha, 500, 1L, loteId);

        verify(loteUseCase).ocuparLote(loteId);
        ArgumentCaptor<EtapaSiembra> etapa = ArgumentCaptor.forClass(EtapaSiembra.class);
        verify(etapaRepository).save(etapa.capture());
        assertEquals(7L, etapa.getValue().getSiembraId());
        assertEquals(EstadoCultivo.GERMINACION, etapa.getValue().getEtapa());
        assertEquals(fecha, etapa.getValue().getFecha());
    }

    @Test
    void deberiaRechazarVariedadInexistente() {
        when(catalogo.buscarVariedad(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.registrarSiembra(fecha, 500, 99L, loteId));
        verify(siembraRepository, never()).save(any());
    }

    @Test
    void deberiaRechazarVariedadDesactivada() {
        when(catalogo.buscarVariedad(5L)).thenReturn(Optional.of(
                new VariedadCultivo(5L, "Caturra", false, 2L, "Café", true)));

        assertThrows(BusinessException.class, () ->
                service.registrarSiembra(fecha, 500, 5L, loteId));
    }

    @Test
    void deberiaRechazarCultivoDesactivado() {
        when(catalogo.buscarVariedad(6L)).thenReturn(Optional.of(
                new VariedadCultivo(6L, "Hass", true, 3L, "Aguacate", false)));

        assertThrows(BusinessException.class, () ->
                service.registrarSiembra(fecha, 500, 6L, loteId));
    }

    @Test
    void deberiaRechazarSiElLoteTieneUnaSiembraEnCurso() {
        when(siembraRepository.findActivaByLoteId(loteId)).thenReturn(Optional.of(
                new Siembra(3L, fecha, 100, Variedades.KENYA, loteId)));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.registrarSiembra(fecha, 500, 1L, loteId));
        assertTrue(ex.getMessage().contains("en curso"));
        verify(siembraRepository, never()).save(any());
    }

    @Test
    void sePuedeSembrarDeNuevoCuandoLaAnteriorTermino() {
        // findActivaByLoteId vacío: la siembra anterior está finalizada
        Siembra resultado = service.registrarSiembra(fecha, 300, 1L, loteId);

        assertNotNull(resultado);
        assertEquals(EstadoCultivo.GERMINACION, resultado.getEstadoCultivo());
    }

    // ── buscar ────────────────────────────────────────────────────────

    @Test
    void deberiaBuscarSiembraPorId() {
        siembraEn(EstadoCultivo.GERMINACION);

        assertEquals(1L, service.buscarSiembraPorId(1L).getId());
    }

    @Test
    void deberiaLanzarExcepcionSiSiembraNoExiste() {
        when(siembraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.buscarSiembraPorId(99L));
    }

    @Test
    void deberiaBuscarLaSiembraEnCursoDelLote() {
        Siembra siembra = new Siembra(1L, fecha, 500, Variedades.GIGANTE, loteId);
        when(siembraRepository.findActivaByLoteId(loteId)).thenReturn(Optional.of(siembra));

        assertEquals(loteId, service.buscarSiembraPorLote(loteId).getLoteId());
    }

    @Test
    void deberiaLanzarExcepcionSiNoHaySiembraEnLote() {
        assertThrows(ResourceNotFoundException.class, () -> service.buscarSiembraPorLote(99L));
    }

    @Test
    void deberiaListarElHistorialDelLote() {
        when(siembraRepository.findHistorialByLoteId(loteId)).thenReturn(List.of(
                new Siembra(2L, fecha, 100, Variedades.GIGANTE, loteId),
                new Siembra(1L, fecha.minusYears(1), 100, Variedades.KENYA, loteId)));

        assertEquals(2, service.listarHistorialPorLote(loteId).size());
    }

    // ── etapas ────────────────────────────────────────────────────────

    @Test
    void deberiaAvanzarEtapaYGuardarLaFecha() {
        siembraEn(EstadoCultivo.GERMINACION);
        LocalDate dia = LocalDate.now().minusDays(1);

        Siembra resultado = service.avanzarEtapa(1L, dia);

        assertEquals(EstadoCultivo.CRECIMIENTO, resultado.getEstadoCultivo());
        ArgumentCaptor<EtapaSiembra> etapa = ArgumentCaptor.forClass(EtapaSiembra.class);
        verify(etapaRepository).save(etapa.capture());
        assertEquals(EstadoCultivo.CRECIMIENTO, etapa.getValue().getEtapa());
        assertEquals(dia, etapa.getValue().getFecha());
        verify(loteUseCase, never()).liberarLote(any(), any());
    }

    @Test
    void sinFechaElCambioDeEtapaQuedaConLaDeHoy() {
        siembraEn(EstadoCultivo.GERMINACION);

        service.avanzarEtapa(1L, null);

        ArgumentCaptor<EtapaSiembra> etapa = ArgumentCaptor.forClass(EtapaSiembra.class);
        verify(etapaRepository).save(etapa.capture());
        assertEquals(LocalDate.now(), etapa.getValue().getFecha());
    }

    @Test
    void noSeAceptaUnaFechaAnteriorALaEtapaAnterior() {
        siembraEn(EstadoCultivo.CRECIMIENTO);
        when(etapaRepository.findUltima(1L)).thenReturn(Optional.of(
                new EtapaSiembra(4L, 1L, EstadoCultivo.CRECIMIENTO, LocalDate.of(2026, 3, 1))));

        assertThrows(BusinessException.class, () -> service.avanzarEtapa(1L, LocalDate.of(2026, 2, 1)));
    }

    @Test
    void noSeAceptaUnaFechaFutura() {
        siembraEn(EstadoCultivo.GERMINACION);

        assertThrows(BusinessException.class, () -> service.avanzarEtapa(1L, LocalDate.now().plusDays(1)));
    }

    @Test
    void alAvanzarDesdeCosechaSeFinalizaYElLoteQuedaDisponible() {
        siembraEn(EstadoCultivo.COSECHA);

        Siembra resultado = service.avanzarEtapa(1L, null);

        assertEquals(EstadoCultivo.FINALIZADO, resultado.getEstadoCultivo());
        assertEquals(LocalDate.now(), resultado.getFechaFin());
        verify(loteUseCase).liberarLote(loteId, EstadoLote.DISPONIBLE);
    }

    @Test
    void noSePuedeAvanzarUnaSiembraFinalizada() {
        siembraEn(EstadoCultivo.FINALIZADO);

        assertThrows(BusinessException.class, () -> service.avanzarEtapa(1L, null));
    }

    @Test
    void retrocederBorraElUltimoCambioDeEtapa() {
        siembraEn(EstadoCultivo.PRODUCCION);
        when(etapaRepository.findUltima(1L)).thenReturn(Optional.of(
                new EtapaSiembra(9L, 1L, EstadoCultivo.PRODUCCION, fecha.plusDays(60))));

        Siembra resultado = service.retrocederEtapa(1L);

        assertEquals(EstadoCultivo.CRECIMIENTO, resultado.getEstadoCultivo());
        verify(etapaRepository).deleteById(9L);
    }

    @Test
    void retrocederUnaFinalizadaLaReabreYOcupaElLote() {
        Siembra s = siembraEn(EstadoCultivo.FINALIZADO);
        s.setFechaFin(fecha.plusDays(200));
        when(siembraRepository.existsActivaByLoteId(loteId)).thenReturn(false);

        Siembra resultado = service.retrocederEtapa(1L);

        assertEquals(EstadoCultivo.COSECHA, resultado.getEstadoCultivo());
        assertNull(resultado.getFechaFin());
        verify(loteUseCase).ocuparLote(loteId);
    }

    @Test
    void noSeReabreSiElLoteYaTieneOtraSiembraEnCurso() {
        siembraEn(EstadoCultivo.FINALIZADO);
        when(siembraRepository.existsActivaByLoteId(loteId)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.retrocederEtapa(1L));
        verify(siembraRepository, never()).save(any());
    }

    @Test
    void noSePuedeRetrocederDesdeGerminacion() {
        siembraEn(EstadoCultivo.GERMINACION);

        assertThrows(BusinessException.class, () -> service.retrocederEtapa(1L));
    }

    @Test
    void finalizarDejaElLoteEnDescansoSiSePide() {
        siembraEn(EstadoCultivo.CRECIMIENTO);
        LocalDate dia = LocalDate.now().minusDays(2);

        Siembra resultado = service.finalizarSiembra(1L, dia, true);

        assertEquals(EstadoCultivo.FINALIZADO, resultado.getEstadoCultivo());
        assertEquals(dia, resultado.getFechaFin());
        verify(loteUseCase).liberarLote(loteId, EstadoLote.EN_DESCANSO);
    }

    // ── editar ────────────────────────────────────────────────────────

    @Test
    void editarCambiaPlantasFechaYVariedad() {
        siembraEn(EstadoCultivo.GERMINACION);
        EtapaSiembra germinacion = new EtapaSiembra(3L, 1L, EstadoCultivo.GERMINACION, fecha);
        when(etapaRepository.findBySiembraId(1L)).thenReturn(List.of(germinacion));
        when(catalogo.buscarVariedad(2L)).thenReturn(Optional.of(Variedades.GIGANTE));

        Siembra resultado = service.actualizarSiembra(1L, fecha.minusDays(3), 650, 2L);

        assertEquals(650, resultado.getCantidadPlantas());
        assertEquals(fecha.minusDays(3), resultado.getFechaSiembra());
        assertEquals(Variedades.GIGANTE.getNombre(), resultado.getVariedad());
        assertEquals(fecha.minusDays(3), germinacion.getFecha());
        verify(etapaRepository).save(germinacion);
    }

    @Test
    void editarNoAceptaUnaFechaDeSiembraPosteriorAUnCambioDeEtapa() {
        siembraEn(EstadoCultivo.CRECIMIENTO);
        when(etapaRepository.findBySiembraId(1L)).thenReturn(List.of(
                new EtapaSiembra(3L, 1L, EstadoCultivo.GERMINACION, fecha),
                new EtapaSiembra(4L, 1L, EstadoCultivo.CRECIMIENTO, fecha.plusDays(20))));

        assertThrows(BusinessException.class, () ->
                service.actualizarSiembra(1L, fecha.plusDays(30), null, null));
    }

    // ── otros ─────────────────────────────────────────────────────────

    @Test
    void deberiaListarSiembrasPorEstado() {
        when(siembraRepository.findByEstadoCultivo(EstadoCultivo.GERMINACION)).thenReturn(List.of(
                new Siembra(1L, fecha, 100, Variedades.COLOMBIA, loteId),
                new Siembra(2L, fecha, 200, Variedades.GIGANTE, 2L)));

        assertEquals(2, service.listarSiembrasPorEstado("GERMINACION").size());
    }

    @Test
    void eliminarEnCursoLaCierraHoyConservaSusRegistrosYLiberaElLote() {
        siembraEn(EstadoCultivo.PRODUCCION);
        when(siembraRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.eliminarSiembra(1L);

        ArgumentCaptor<Siembra> guardada = ArgumentCaptor.forClass(Siembra.class);
        verify(siembraRepository).save(guardada.capture());
        assertTrue(guardada.getValue().estaEliminada());
        assertEquals(EstadoCultivo.FINALIZADO, guardada.getValue().getEstadoCultivo());
        assertEquals(LocalDate.now(), guardada.getValue().getFechaFin());
        verify(etapaRepository).save(argThat(e -> e.getEtapa() == EstadoCultivo.FINALIZADO));
        verify(loteUseCase).liberarLote(loteId, EstadoLote.DISPONIBLE);
        // Borrado lógico: los gastos e ingresos (actividades, fumigaciones, cosechas) se conservan
        verifyNoInteractions(cosechaRepository, fumigacionRepository, actividadRepository);
        verify(etapaRepository, never()).deleteBySiembraId(any());
    }

    @Test
    void eliminarUnaTemporadaTerminadaNoTocaElLoteNiSuFechaDeFin() {
        Siembra s = siembraEn(EstadoCultivo.FINALIZADO);
        LocalDate fin = fecha.plusDays(90);
        s.setFechaFin(fin);
        when(siembraRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.eliminarSiembra(1L);

        assertTrue(s.estaEliminada());
        assertEquals(fin, s.getFechaFin());
        verify(etapaRepository, never()).save(any());
        verify(loteUseCase, never()).liberarLote(any(), any());
    }

    @Test
    void eliminarDosVecesNoHaceNada() {
        Siembra s = siembraEn(EstadoCultivo.FINALIZADO);
        s.setEliminadoEn(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));

        service.eliminarSiembra(1L);

        verify(siembraRepository, never()).save(any());
    }

    @Test
    void deberiaLanzarExcepcionAlEliminarSiembraInexistente() {
        when(siembraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarSiembra(99L));
    }
}
