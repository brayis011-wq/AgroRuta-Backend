package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.in.SiembraUseCase;
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

    @InjectMocks
    private FumigacionService service;

    private final Long siembraId = 1L;
    private final LocalDate fecha = LocalDate.of(2026, 3, 10);

    @BeforeEach
    void setUp() {
        Siembra siembra = new Siembra(siembraId, fecha, 100, VariedadUchuva.COLOMBIA, 1L);
        when(siembraUseCase.buscarSiembraPorId(siembraId)).thenReturn(siembra);
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
}