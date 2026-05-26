package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.ports.in.CosechaUseCase;
import com.agroruta.crop.domain.CalidadCosecha;
import com.agroruta.crop.domain.Cosecha;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CosechaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CosechaUseCase cosechaUseCase;

    @InjectMocks
    private CosechaController controller;

    private final LocalDate FECHA = LocalDate.of(2025, 1, 15);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  POST /api/cosechas
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaRegistrarCosechaYRetornar201() throws Exception {
        Cosecha cosecha = new Cosecha(
                1L, FECHA, 150.5, CalidadCosecha.PRIMERA, "Cosecha en buen estado", 10L
        );
        when(cosechaUseCase.registrarCosecha(any(), anyDouble(), anyString(), anyString(), anyLong()))
                .thenReturn(cosecha);

        String body = """
                {
                    "fecha": "%s",
                    "cantidadKg": 150.5,
                    "calidad": "PRIMERA",
                    "observaciones": "Cosecha en buen estado",
                    "siembraId": 10
                }
                """.formatted(FECHA);

        mockMvc.perform(post("/api/cosechas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.cantidadKg").value(150.5))
                .andExpect(jsonPath("$.calidad").value("PRIMERA"))
                .andExpect(jsonPath("$.observaciones").value("Cosecha en buen estado"))
                .andExpect(jsonPath("$.siembraId").value(10));
    }

    @Test
    void deberiaRegistrarCosechaDeSegundaCalidad() throws Exception {
        Cosecha cosecha = new Cosecha(
                2L, FECHA, 80.0, CalidadCosecha.SEGUNDA, "Leve daño por lluvia", 10L
        );
        when(cosechaUseCase.registrarCosecha(any(), anyDouble(), anyString(), anyString(), anyLong()))
                .thenReturn(cosecha);

        String body = """
                {
                    "fecha": "%s",
                    "cantidadKg": 80.0,
                    "calidad": "SEGUNDA",
                    "observaciones": "Leve daño por lluvia",
                    "siembraId": 10
                }
                """.formatted(FECHA);

        mockMvc.perform(post("/api/cosechas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.calidad").value("SEGUNDA"))
                .andExpect(jsonPath("$.cantidadKg").value(80.0));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  GET /api/cosechas/siembra/{siembraId}
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaListarCosechasPorSiembra() throws Exception {
        List<Cosecha> cosechas = List.of(
                new Cosecha(1L, FECHA,                        150.5, CalidadCosecha.PRIMERA, "Cosecha principal",  10L),
                new Cosecha(2L, FECHA.plusDays(15), 60.0,  CalidadCosecha.TERCERA, "Cosecha rezagada",  10L)
        );
        when(cosechaUseCase.listarCosechasPorSiembra(10L)).thenReturn(cosechas);

        mockMvc.perform(get("/api/cosechas/siembra/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].calidad").value("PRIMERA"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].calidad").value("TERCERA"));
    }

    @Test
    void deberiaRetornarListaVaciaCuandoSiembraSinCosechas() throws Exception {
        when(cosechaUseCase.listarCosechasPorSiembra(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/cosechas/siembra/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  GET /api/cosechas/siembra/{siembraId}/total-kg
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaRetornarTotalKgCosechadoPorSiembra() throws Exception {
        when(cosechaUseCase.totalKgCosechado(10L)).thenReturn(210.5);

        mockMvc.perform(get("/api/cosechas/siembra/10/total-kg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(210.5));
    }

    @Test
    void deberiaRetornarCeroKgCuandoNoHayCosechas() throws Exception {
        when(cosechaUseCase.totalKgCosechado(99L)).thenReturn(0.0);

        mockMvc.perform(get("/api/cosechas/siembra/99/total-kg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0.0));
    }
}