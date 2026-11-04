package com.agroruta.crop.application;

import com.agroruta.crop.domain.CatalogoCultivoRepository;
import com.agroruta.crop.domain.TipoCultivo;
import com.agroruta.crop.domain.VariedadCultivo;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoCultivoServiceTest {

    @Mock private CatalogoCultivoRepository catalogo;
    @InjectMocks private CatalogoCultivoService service;

    @Test
    void crearTipoLimpiaElNombre() {
        when(catalogo.existeTipo("Café")).thenReturn(false);
        when(catalogo.guardarTipo(any())).thenAnswer(inv -> inv.getArgument(0));

        TipoCultivo t = service.crearTipo("  café  ");

        assertEquals("Café", t.getNombre());
        assertTrue(t.isActivo());
    }

    @Test
    void crearTipoRechazaDuplicado() {
        when(catalogo.existeTipo("Café")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.crearTipo("café"));
        verify(catalogo, never()).guardarTipo(any());
    }

    @Test
    void crearTipoRechazaNombreVacio() {
        assertThrows(IllegalArgumentException.class, () -> service.crearTipo("   "));
    }

    @Test
    void crearVariedadExigeCultivoExistente() {
        when(catalogo.buscarTipo(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.crearVariedad(9L, "Hass"));
    }

    @Test
    void crearVariedadRechazaDuplicadoEnElMismoCultivo() {
        when(catalogo.buscarTipo(3L)).thenReturn(Optional.of(new TipoCultivo(3L, "Aguacate", true, new ArrayList<>())));
        when(catalogo.existeVariedad(3L, "Hass")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.crearVariedad(3L, "hass"));
    }

    @Test
    void cambiarEstadoVariedadLaDesactiva() {
        VariedadCultivo v = new VariedadCultivo(5L, "Caturra", true, 2L, "Café", true);
        when(catalogo.buscarVariedad(5L)).thenReturn(Optional.of(v));
        when(catalogo.guardarVariedad(any())).thenAnswer(inv -> inv.getArgument(0));

        assertFalse(service.cambiarEstadoVariedad(5L, false).isActivo());
    }
}
