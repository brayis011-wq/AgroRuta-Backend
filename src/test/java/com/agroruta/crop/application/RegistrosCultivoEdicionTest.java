package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.domain.*;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Edición y borrado de cosechas, fumigaciones y actividades, y bloqueo en siembras finalizadas. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrosCultivoEdicionTest {

    @Mock private CosechaRepository cosechaRepository;
    @Mock private FumigacionRepository fumigacionRepository;
    @Mock private ActividadCultivoRepository actividadRepository;
    @Mock private SiembraUseCase siembraUseCase;

    private CosechaService cosechas;
    private FumigacionService fumigaciones;
    private ActividadCultivoService actividades;

    private final LocalDate fecha = LocalDate.of(2026, 5, 10);
    private Siembra finalizada;

    @BeforeEach
    void setUp() {
        cosechas = new CosechaService(cosechaRepository, siembraUseCase);
        fumigaciones = new FumigacionService(fumigacionRepository, siembraUseCase,
                org.mockito.Mockito.mock(com.agroruta.crop.application.ports.out.InventarioPort.class),
                org.mockito.Mockito.mock(LoteRepository.class), org.mockito.Mockito.mock(FincaRepository.class));
        actividades = new ActividadCultivoService(actividadRepository, siembraUseCase);

        finalizada = new Siembra(5L, fecha.minusMonths(6), 100, Variedades.COLOMBIA, 1L);
        finalizada.finalizar(fecha);
        when(siembraUseCase.buscarSiembraPorId(5L)).thenReturn(finalizada);

        when(cosechaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(fumigacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(actividadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void enUnaSiembraFinalizadaNoSeRegistraNadaNuevo() {
        assertThrows(BusinessException.class, () ->
                cosechas.registrarCosecha(fecha, 10.0, "PRIMERA", null, 5L));
        assertThrows(BusinessException.class, () ->
                fumigaciones.registrarFumigacion(fecha, "Mancozeb", null, 2.0, "LITROS", 1.0, null, 5L));
        assertThrows(BusinessException.class, () ->
                actividades.registrarActividad("PODA", "Poda de formación", fecha, 5L));
        verifyNoInteractions(cosechaRepository, fumigacionRepository, actividadRepository);
    }

    @Test
    void editarCosechaCambiaKilosCalidadYFecha() {
        Cosecha c = new Cosecha(1L, fecha, 20.0, CalidadCosecha.PRIMERA, "ok", 5L);
        when(cosechaRepository.findById(1L)).thenReturn(Optional.of(c));
        when(cosechaRepository.existsByFechaAndSiembraId(fecha.plusDays(1), 5L)).thenReturn(false);

        Cosecha r = cosechas.actualizarCosecha(1L, fecha.plusDays(1), 25.5, "segunda", null);

        assertEquals(25.5, r.getCantidadKg());
        assertEquals(CalidadCosecha.SEGUNDA, r.getCalidad());
        assertEquals(fecha.plusDays(1), r.getFecha());
        assertEquals(5L, r.getSiembraId());
    }

    @Test
    void editarCosechaNoPuedeQuedarElMismoDiaQueOtra() {
        when(cosechaRepository.findById(1L)).thenReturn(Optional.of(
                new Cosecha(1L, fecha, 20.0, CalidadCosecha.PRIMERA, null, 5L)));
        when(cosechaRepository.existsByFechaAndSiembraId(fecha.plusDays(2), 5L)).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                cosechas.actualizarCosecha(1L, fecha.plusDays(2), 20.0, "PRIMERA", null));
    }

    @Test
    void editarCosechaSinCambiarLaFechaNoChocaConsigoMisma() {
        when(cosechaRepository.findById(1L)).thenReturn(Optional.of(
                new Cosecha(1L, fecha, 20.0, CalidadCosecha.PRIMERA, null, 5L)));
        when(cosechaRepository.existsByFechaAndSiembraId(fecha, 5L)).thenReturn(true);

        assertEquals(30.0, cosechas.actualizarCosecha(1L, fecha, 30.0, "PRIMERA", null).getCantidadKg());
    }

    @Test
    void eliminarCosechaInexistenteDa404() {
        when(cosechaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cosechas.eliminarCosecha(9L));
        verify(cosechaRepository, never()).deleteById(any());
    }

    @Test
    void editarFumigacionEscritaAManoDejaDeApuntarAlCatalogo() {
        Fumigacion f = Fumigacion.fromCatalog(fecha, "Mancozeb", 3L, 2.0, UnidadMedida.LITROS, 1.0, null, 5L);
        f.setId(1L);
        when(fumigacionRepository.findById(1L)).thenReturn(Optional.of(f));

        Fumigacion r = fumigaciones.actualizarFumigacion(1L, fecha, "Caldo bordelés", null, 3.0, "litros", 1.5, "x");

        assertEquals("Caldo bordelés", r.getProducto());
        assertNull(r.getAgriculturalInputId());
        assertEquals(3.0, r.getDosis());
        assertEquals(1.5, r.getAreaAplicada());
    }

    @Test
    void eliminarFumigacion() {
        when(fumigacionRepository.findById(1L)).thenReturn(Optional.of(new Fumigacion()));

        fumigaciones.eliminarFumigacion(1L);

        verify(fumigacionRepository).deleteById(1L);
    }

    @Test
    void editarActividadValidaQueNoSeRepitaTipoYFecha() {
        when(actividadRepository.findById(1L)).thenReturn(Optional.of(
                new ActividadCultivo(1L, TipoActividad.PODA, "Poda", fecha, 5L)));
        when(actividadRepository.existsByTipoAndFechaAndSiembraId(TipoActividad.RIEGO, fecha, 5L)).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                actividades.actualizarActividad(1L, "RIEGO", "Riego", fecha));
    }

    @Test
    void editarActividadCambiaDescripcion() {
        when(actividadRepository.findById(1L)).thenReturn(Optional.of(
                new ActividadCultivo(1L, TipoActividad.PODA, "Poda", fecha, 5L)));

        ActividadCultivo r = actividades.actualizarActividad(1L, "PODA", "  Poda sanitaria ", fecha);

        assertEquals("Poda sanitaria", r.getDescripcion());
        verify(actividadRepository, never()).existsByTipoAndFechaAndSiembraId(any(), any(), any());
    }
}
