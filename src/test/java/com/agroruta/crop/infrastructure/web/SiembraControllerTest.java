package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.domain.EstadoCultivo;
import com.agroruta.crop.domain.Siembra;
import com.agroruta.crop.domain.VariedadUchuva;
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
    //  POST /api/siembras
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaRegistrarSiembraYRetornar201() throws Exception {
        Siembra siembra = new Siembra(1L, FECHA_SIEMBRA, 500, VariedadUchuva.COLOMBIA, 3L);
        when(siembraUseCase.registrarSiembra(any(), anyInt(), anyString(), anyLong()))
                .thenReturn(siembra);

        String body = """
                {
                    "fechaSiembra": "%s",
                    "cantidadPlantas": 500,
                    "variedad": "COLOMBIA",
                    "loteId": 3
                }
                """.formatted(FECHA_SIEMBRA);

        mockMvc.perform(post("/api/siembras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.cantidadPlantas").value(500))
                .andExpect(jsonPath("$.variedad").value("COLOMBIA"))
                .andExpect(jsonPath("$.estadoCultivo").value("GERMINACION"))
                .andExpect(jsonPath("$.loteId").value(3));
    }

    @Test
    void deberiaRegistrarSiembraConVariedadKenya() throws Exception {
        Siembra siembra = new Siembra(2L, FECHA_SIEMBRA, 300, VariedadUchuva.KENYA, 4L);
        when(siembraUseCase.registrarSiembra(any(), anyInt(), anyString(), anyLong()))
                .thenReturn(siembra);

        String body = """
                {
                    "fechaSiembra": "%s",
                    "cantidadPlantas": 300,
                    "variedad": "KENYA",
                    "loteId": 4
                }
                """.formatted(FECHA_SIEMBRA);

        mockMvc.perform(post("/api/siembras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.variedad").value("KENYA"))
                .andExpect(jsonPath("$.estadoCultivo").value("GERMINACION"));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  GET /api/siembras/{id}
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaBuscarSiembraPorIdYRetornar200() throws Exception {
        Siembra siembra = new Siembra(1L, FECHA_SIEMBRA, 500, VariedadUchuva.COLOMBIA, 3L);
        when(siembraUseCase.buscarSiembraPorId(1L)).thenReturn(siembra);

        mockMvc.perform(get("/api/siembras/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.variedad").value("COLOMBIA"))
                .andExpect(jsonPath("$.estadoCultivo").value("GERMINACION"))
                .andExpect(jsonPath("$.loteId").value(3));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  GET /api/siembras/lote/{loteId}
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaBuscarSiembraPorLoteYRetornar200() throws Exception {
        Siembra siembra = new Siembra(1L, FECHA_SIEMBRA, 500, VariedadUchuva.COLOMBIA, 3L);
        when(siembraUseCase.buscarSiembraPorLote(3L)).thenReturn(siembra);

        mockMvc.perform(get("/api/siembras/lote/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.loteId").value(3))
                .andExpect(jsonPath("$.variedad").value("COLOMBIA"));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  PUT /api/siembras/{id}/avanzar-etapa
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaAvanzarEtapaDeGerminacionACrecimiento() throws Exception {
        Siembra siembra = new Siembra(1L, FECHA_SIEMBRA, 500, VariedadUchuva.COLOMBIA, 3L);
        siembra.avanzarEtapa();  // GERMINACION → CRECIMIENTO
        when(siembraUseCase.avanzarEtapa(1L)).thenReturn(siembra);

        mockMvc.perform(put("/api/siembras/1/avanzar-etapa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.estadoCultivo").value("CRECIMIENTO"));
    }

    @Test
    void deberiaAvanzarEtapaHastaProduccion() throws Exception {
        Siembra siembra = new Siembra(1L, FECHA_SIEMBRA, 500, VariedadUchuva.GIGANTE, 3L);
        siembra.avanzarEtapa();  // GERMINACION → CRECIMIENTO
        siembra.avanzarEtapa();  // CRECIMIENTO → PRODUCCION
        when(siembraUseCase.avanzarEtapa(1L)).thenReturn(siembra);

        mockMvc.perform(put("/api/siembras/1/avanzar-etapa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.estadoCultivo").value("PRODUCCION"));
    }

    @Test
    void deberiaAvanzarEtapaHastaFinalizado() throws Exception {
        Siembra siembra = new Siembra(1L, FECHA_SIEMBRA, 500, VariedadUchuva.KENYA, 3L);
        siembra.avanzarEtapa();  // GERMINACION → CRECIMIENTO
        siembra.avanzarEtapa();  // CRECIMIENTO → PRODUCCION
        siembra.avanzarEtapa();  // PRODUCCION  → COSECHA
        siembra.avanzarEtapa();  // COSECHA     → FINALIZADO
        when(siembraUseCase.avanzarEtapa(1L)).thenReturn(siembra);

        mockMvc.perform(put("/api/siembras/1/avanzar-etapa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.estadoCultivo").value("FINALIZADO"));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  GET /api/siembras/estado/{estado}
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaListarSiembrasPorEstado() throws Exception {
        Siembra s1 = new Siembra(1L, FECHA_SIEMBRA,                500, VariedadUchuva.COLOMBIA, 3L);
        Siembra s2 = new Siembra(2L, FECHA_SIEMBRA.plusDays(10), 200, VariedadUchuva.GIGANTE,  4L);
        when(siembraUseCase.listarSiembrasPorEstado("GERMINACION")).thenReturn(List.of(s1, s2));

        mockMvc.perform(get("/api/siembras/estado/GERMINACION"))
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

        mockMvc.perform(get("/api/siembras/estado/FINALIZADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  DELETE /api/siembras/{id}
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaEliminarSiembraYRetornar204() throws Exception {
        doNothing().when(siembraUseCase).eliminarSiembra(1L);

        mockMvc.perform(delete("/api/siembras/1"))
                .andExpect(status().isNoContent());

        verify(siembraUseCase).eliminarSiembra(1L);
    }
}