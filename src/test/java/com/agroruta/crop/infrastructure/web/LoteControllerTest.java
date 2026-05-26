package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.ports.in.LoteUseCase;
import com.agroruta.crop.domain.EstadoLote;
import com.agroruta.crop.domain.Lote;
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
class LoteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LoteUseCase loteUseCase;

    @InjectMocks
    private LoteController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  POST /api/lotes
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaRegistrarLoteYRetornar201() throws Exception {
        Lote lote = new Lote(1L, "Lote A", 3.5, 2L);
        when(loteUseCase.registrarLote(anyString(), anyDouble(), anyLong()))
                .thenReturn(lote);

        String body = """
                {
                    "nombre": "Lote A",
                    "area": 3.5,
                    "fincaId": 2
                }
                """;

        mockMvc.perform(post("/api/lotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("Lote A"))
                .andExpect(jsonPath("$.area").value(3.5))
                .andExpect(jsonPath("$.fincaId").value(2))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  GET /api/lotes/{id}
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaBuscarLotePorIdYRetornar200() throws Exception {
        Lote lote = new Lote(1L, "Lote A", 3.5, 2L);
        when(loteUseCase.buscarLotePorId(1L)).thenReturn(lote);

        mockMvc.perform(get("/api/lotes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("Lote A"))
                .andExpect(jsonPath("$.area").value(3.5))
                .andExpect(jsonPath("$.fincaId").value(2))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
    }

    @Test
    void deberiaBuscarLoteEnCultivoYRetornar200() throws Exception {
        Lote lote = new Lote(2L, "Lote B", 5.0, 2L);
        lote.iniciarCultivo();
        when(loteUseCase.buscarLotePorId(2L)).thenReturn(lote);

        mockMvc.perform(get("/api/lotes/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.estado").value("EN_CULTIVO"));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  GET /api/lotes/finca/{fincaId}
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaListarLotesPorFinca() throws Exception {
        Lote lote1 = new Lote(1L, "Lote A", 3.5, 2L);
        Lote lote2 = new Lote(2L, "Lote B", 5.0, 2L);
        lote2.iniciarCultivo();

        when(loteUseCase.listarLotesPorFinca(2L)).thenReturn(List.of(lote1, lote2));

        mockMvc.perform(get("/api/lotes/finca/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nombre").value("Lote A"))
                .andExpect(jsonPath("$[0].estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].nombre").value("Lote B"))
                .andExpect(jsonPath("$[1].estado").value("EN_CULTIVO"));
    }

    @Test
    void deberiaRetornarListaVaciaCuandoFincaSinLotes() throws Exception {
        when(loteUseCase.listarLotesPorFinca(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/lotes/finca/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  DELETE /api/lotes/{id}
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaEliminarLoteYRetornar204() throws Exception {
        doNothing().when(loteUseCase).eliminarLote(1L);

        mockMvc.perform(delete("/api/lotes/1"))
                .andExpect(status().isNoContent());

        verify(loteUseCase).eliminarLote(1L);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  PATCH /api/lotes/{id}/geometria
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaActualizarGeometriaYRetornar200() throws Exception {
        Lote lote = new Lote(1L, "Lote A", 4.1, 2L);
        lote.actualizarGeometria(
                "{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[1,0],[1,1],[0,0]]]}",
                4.1, 4.5709, -74.2973
        );
        when(loteUseCase.actualizarGeometriaLote(eq(1L), anyString(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(lote);

        String body = """
                {
                    "coordenadas": "{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[0,0],[1,0],[1,1],[0,0]]]}",
                    "area": 4.1,
                    "centroideLat": 4.5709,
                    "centroideLng": -74.2973
                }
                """;

        mockMvc.perform(patch("/api/lotes/1/geometria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.area").value(4.1))
                .andExpect(jsonPath("$.centroideLat").value(4.5709))
                .andExpect(jsonPath("$.centroideLng").value(-74.2973));
    }

    @Test
    void deberiaActualizarGeometriaDeOtroLote() throws Exception {
        Lote lote = new Lote(3L, "Lote C", 6.8, 2L);
        lote.actualizarGeometria(
                "{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[2,0],[2,2],[0,0]]]}",
                6.8, 4.6012, -74.0839
        );
        when(loteUseCase.actualizarGeometriaLote(eq(3L), anyString(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(lote);

        String body = """
                {
                    "coordenadas": "{\\"type\\":\\"Polygon\\",\\"coordinates\\":[[[0,0],[2,0],[2,2],[0,0]]]}",
                    "area": 6.8,
                    "centroideLat": 4.6012,
                    "centroideLng": -74.0839
                }
                """;

        mockMvc.perform(patch("/api/lotes/3/geometria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.area").value(6.8))
                .andExpect(jsonPath("$.centroideLat").value(4.6012))
                .andExpect(jsonPath("$.centroideLng").value(-74.0839));
    }
}