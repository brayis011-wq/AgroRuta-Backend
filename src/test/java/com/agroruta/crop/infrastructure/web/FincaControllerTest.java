package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.ports.in.FincaUseCase;
import com.agroruta.crop.domain.Finca;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FincaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FincaUseCase fincaUseCase;

    @InjectMocks
    private FincaController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  POST /api/fincas
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaRegistrarFincaYRetornar201() throws Exception {
        Finca finca = new Finca(1L, "La Esperanza", "Vereda El Monte", 12.5, 5L);
        when(fincaUseCase.registrarFinca(anyString(), anyString(), anyDouble(), anyLong()))
                .thenReturn(finca);

        String body = """
                {
                    "nombre": "La Esperanza",
                    "ubicacion": "Vereda El Monte",
                    "hectareas": 12.5,
                    "agricultorId": 5
                }
                """;

        mockMvc.perform(post("/api/fincas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("La Esperanza"))
                .andExpect(jsonPath("$.ubicacion").value("Vereda El Monte"))
                .andExpect(jsonPath("$.hectareas").value(12.5))
                .andExpect(jsonPath("$.agricultorId").value(5));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  GET /api/fincas/{id}
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaBuscarFincaPorIdYRetornar200() throws Exception {
        Finca finca = new Finca(1L, "La Esperanza", "Vereda El Monte", 12.5, 5L);
        when(fincaUseCase.buscarFincaPorId(1L)).thenReturn(finca);

        mockMvc.perform(get("/api/fincas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("La Esperanza"))
                .andExpect(jsonPath("$.hectareas").value(12.5))
                .andExpect(jsonPath("$.agricultorId").value(5));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  GET /api/fincas/agricultor/{agricultorId}
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaListarFincasPorAgricultor() throws Exception {
        List<Finca> fincas = List.of(
                new Finca(1L, "La Esperanza",  "Vereda El Monte",  12.5, 5L),
                new Finca(2L, "El Paraíso",    "Vereda La Palma",   8.0, 5L)
        );
        when(fincaUseCase.listarFincasPorAgricultor(5L)).thenReturn(fincas);

        mockMvc.perform(get("/api/fincas/agricultor/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nombre").value("La Esperanza"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].nombre").value("El Paraíso"));
    }

    @Test
    void deberiaRetornarListaVaciaCuandoAgricultorSinFincas() throws Exception {
        when(fincaUseCase.listarFincasPorAgricultor(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/fincas/agricultor/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  DELETE /api/fincas/{id}
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaEliminarFincaYRetornar204() throws Exception {
        doNothing().when(fincaUseCase).eliminarFinca(1L);

        mockMvc.perform(delete("/api/fincas/1"))
                .andExpect(status().isNoContent());

        verify(fincaUseCase).eliminarFinca(1L);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  PATCH /api/fincas/{id}/centroide
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaActualizarCentroideYRetornar200() throws Exception {
        Finca finca = new Finca(1L, "La Esperanza", "Vereda El Monte", 12.5, 5L);
        finca.actualizarCentroide(4.5709, -74.2973);
        when(fincaUseCase.actualizarCentroideFinca(eq(1L), anyDouble(), anyDouble()))
                .thenReturn(finca);

        String body = """
                {
                    "centroideLat": 4.5709,
                    "centroideLng": -74.2973
                }
                """;

        mockMvc.perform(patch("/api/fincas/1/centroide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.centroideLat").value(4.5709))
                .andExpect(jsonPath("$.centroideLng").value(-74.2973));
    }

    @Test
    void deberiaActualizarCentroideDeOtraFinca() throws Exception {
        Finca finca = new Finca(2L, "El Paraíso", "Vereda La Palma", 8.0, 5L);
        finca.actualizarCentroide(4.6012, -74.0839);
        when(fincaUseCase.actualizarCentroideFinca(eq(2L), anyDouble(), anyDouble()))
                .thenReturn(finca);

        String body = """
                {
                    "centroideLat": 4.6012,
                    "centroideLng": -74.0839
                }
                """;

        mockMvc.perform(patch("/api/fincas/2/centroide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.centroideLat").value(4.6012))
                .andExpect(jsonPath("$.centroideLng").value(-74.0839));
    }
}