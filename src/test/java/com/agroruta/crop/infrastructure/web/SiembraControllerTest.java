package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.domain.EstadoCultivo;
import com.agroruta.crop.domain.Siembra;
import com.agroruta.crop.domain.Variedades;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SiembraControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SiembraUseCase siembraUseCase;

    @InjectMocks
    private SiembraController controller;

    private final LocalDate FECHA_SIEMBRA = LocalDate.of(2025, 2, 1);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  POST /api/plantings
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaRegistrarSiembraYRetornar201() throws Exception {
        Siembra siembra = new Siembra(1L, FECHA_SIEMBRA, 500, Variedades.COLOMBIA, 3L);
        when(siembraUseCase.registrarSiembra(any(), anyInt(), anyLong(), anyLong()))
                .thenReturn(siembra);

        String body = """
                {
                    "fechaSiembra": "%s",
                    "cantidadPlantas": 500,
                    "variedadId": 1,
                    "loteId": 3
                }
                """.formatted(FECHA_SIEMBRA);

        mockMvc.perform(post("/api/plantings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.cantidadPlantas").value(500))
                .andExpect(jsonPath("$.variedad").value("Colombia"))
                .andExpect(jsonPath("$.estadoCultivo").value("GERMINACION"))
                .andExpect(jsonPath("$.loteId").value(3));
    }

    @Test
    void deberiaRegistrarSiembraConVariedadKenya() throws Exception {
        Siembra siembra = new Siembra(2L, FECHA_SIEMBRA, 300, Variedades.KENYA, 4L);
        when(siembraUseCase.registrarSiembra(any(), anyInt(), anyLong(), anyLong()))
                .thenReturn(siembra);

        String body = """
                {
                    "fechaSiembra": "%s",
                    "cantidadPlantas": 300,
                    "variedadId": 3,
                    "loteId": 4
                }
                """.formatted(FECHA_SIEMBRA);

        mockMvc.perform(post("/api/plantings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.variedad").value("Kenya"))
                .andExpect(jsonPath("$.estadoCultivo").value("GERMINACION"));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  GET /api/plantings/{id}
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaBuscarSiembraPorIdYRetornar200() throws Exception {
        Siembra siembra = new Siembra(1L, FECHA_SIEMBRA, 500, Variedades.COLOMBIA, 3L);
        when(siembraUseCase.buscarSiembraPorId(1L)).thenReturn(siembra);

        mockMvc.perform(get("/api/plantings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.variedad").value("Colombia"))
                .andExpect(jsonPath("$.estadoCultivo").value("GERMINACION"))
                .andExpect(jsonPath("$.loteId").value(3));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  GET /api/plantings/plot/{loteId}
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaBuscarSiembraPorLoteYRetornar200() throws Exception {
        Siembra siembra = new Siembra(1L, FECHA_SIEMBRA, 500, Variedades.COLOMBIA, 3L);
        when(siembraUseCase.buscarSiembraPorLote(3L)).thenReturn(siembra);

        mockMvc.perform(get("/api/plantings/plot/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.loteId").value(3))
                .andExpect(jsonPath("$.variedad").value("Colombia"));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  PUT /api/plantings/{id}/advance-stage
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaAvanzarEtapaDeGerminacionACrecimiento() throws Exception {
        Siembra siembra = new Siembra(1L, FECHA_SIEMBRA, 500, Variedades.COLOMBIA, 3L);
        siembra.avanzarEtapa();  // GERMINACION → CRECIMIENTO
        when(siembraUseCase.avanzarEtapa(1L, null)).thenReturn(siembra);

        mockMvc.perform(patch("/api/plantings/1/advance-stage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.estadoCultivo").value("CRECIMIENTO"));
    }

    @Test
    void deberiaAvanzarEtapaHastaProduccion() throws Exception {
        Siembra siembra = new Siembra(1L, FECHA_SIEMBRA, 500, Variedades.GIGANTE, 3L);
        siembra.avanzarEtapa();  // GERMINACION → CRECIMIENTO
        siembra.avanzarEtapa();  // CRECIMIENTO → PRODUCCION
        when(siembraUseCase.avanzarEtapa(1L, null)).thenReturn(siembra);

        mockMvc.perform(patch("/api/plantings/1/advance-stage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.estadoCultivo").value("PRODUCCION"));
    }

    @Test
    void deberiaAvanzarEtapaHastaFinalizado() throws Exception {
        Siembra siembra = new Siembra(1L, FECHA_SIEMBRA, 500, Variedades.KENYA, 3L);
        siembra.avanzarEtapa();  // GERMINACION → CRECIMIENTO
        siembra.avanzarEtapa();  // CRECIMIENTO → PRODUCCION
        siembra.avanzarEtapa();  // PRODUCCION  → COSECHA
        siembra.avanzarEtapa();  // COSECHA     → FINALIZADO
        when(siembraUseCase.avanzarEtapa(1L, null)).thenReturn(siembra);

        mockMvc.perform(patch("/api/plantings/1/advance-stage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.estadoCultivo").value("FINALIZADO"));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  GET /api/plantings/status/{estado}
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaListarSiembrasPorEstado() throws Exception {
        Siembra s1 = new Siembra(1L, FECHA_SIEMBRA,                500, Variedades.COLOMBIA, 3L);
        Siembra s2 = new Siembra(2L, FECHA_SIEMBRA.plusDays(10), 200, Variedades.GIGANTE,  4L);
        when(siembraUseCase.listarSiembrasPorEstado("GERMINACION")).thenReturn(List.of(s1, s2));

        mockMvc.perform(get("/api/plantings/status/GERMINACION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].estadoCultivo").value("GERMINACION"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].estadoCultivo").value("GERMINACION"));
    }

    @Test
    void deberiaRetornarListaVaciaCuandoNoHaySiembrasEnEseEstado() throws Exception {
        when(siembraUseCase.listarSiembrasPorEstado("FINALIZADO")).thenReturn(List.of());

        mockMvc.perform(get("/api/plantings/status/FINALIZADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  DELETE /api/plantings/{id}
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaEliminarSiembraYRetornar204() throws Exception {
        doNothing().when(siembraUseCase).eliminarSiembra(1L);

        mockMvc.perform(delete("/api/plantings/1"))
                .andExpect(status().isNoContent());

        verify(siembraUseCase).eliminarSiembra(1L);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Temporadas y etapas
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void avanzarEtapaAceptaLaFechaDelCambio() throws Exception {
        Siembra siembra = new Siembra(1L, FECHA_SIEMBRA, 500, Variedades.COLOMBIA, 3L);
        siembra.avanzarEtapa();
        when(siembraUseCase.avanzarEtapa(1L, LocalDate.of(2025, 3, 1))).thenReturn(siembra);

        mockMvc.perform(patch("/api/plantings/1/advance-stage").param("fecha", "2025-03-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoCultivo").value("CRECIMIENTO"));
    }

    @Test
    void finalizarConDescansoRetorna200() throws Exception {
        Siembra siembra = new Siembra(1L, FECHA_SIEMBRA, 500, Variedades.COLOMBIA, 3L);
        siembra.finalizar(LocalDate.of(2025, 9, 1));
        when(siembraUseCase.finalizarSiembra(1L, LocalDate.of(2025, 9, 1), true)).thenReturn(siembra);

        mockMvc.perform(patch("/api/plantings/1/finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\": \"2025-09-01\", \"dejarEnDescanso\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoCultivo").value("FINALIZADO"));
    }

    @Test
    void historialDelLoteDevuelveTodasLasTemporadas() throws Exception {
        when(siembraUseCase.listarHistorialPorLote(3L)).thenReturn(List.of(
                new Siembra(2L, FECHA_SIEMBRA, 500, Variedades.GIGANTE, 3L),
                new Siembra(1L, FECHA_SIEMBRA.minusYears(1), 400, Variedades.COLOMBIA, 3L)));

        mockMvc.perform(get("/api/plantings/plot/3/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2));
    }
}
