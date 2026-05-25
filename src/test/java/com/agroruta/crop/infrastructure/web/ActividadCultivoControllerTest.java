package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.ports.in.ActividadCultivoUseCase;
import com.agroruta.crop.domain.ActividadCultivo;
import com.agroruta.crop.domain.TipoActividad;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class ActividadCultivoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ActividadCultivoUseCase actividadUseCase;

    @InjectMocks
    private ActividadCultivoController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void deberiaRegistrarActividadYRetornar201() throws Exception {
        ActividadCultivo actividad = new ActividadCultivo(
                1L, TipoActividad.RIEGO, "Riego matutino", LocalDate.now(), 10L
        );
        when(actividadUseCase.registrarActividad(any(), any(), any(), anyLong()))
                .thenReturn(actividad);

        String body = """
                {
                    "tipo": "RIEGO",
                    "descripcion": "Riego matutino",
                    "fecha": "%s",
                    "siembraId": 10
                }
                """.formatted(LocalDate.now());

        mockMvc.perform(post("/api/actividades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.tipo").value("RIEGO"));
    }

    @Test
    void deberiaListarActividadesPorSiembra() throws Exception {
        List<ActividadCultivo> actividades = List.of(
                new ActividadCultivo(1L, TipoActividad.RIEGO, "Riego matutino", LocalDate.now(), 10L),
                new ActividadCultivo(2L, TipoActividad.OTRO, "Fumigación preventiva", LocalDate.now(), 10L)
        );
        when(actividadUseCase.listarActividadesPorSiembra(10L)).thenReturn(actividades);

        mockMvc.perform(get("/api/actividades/siembra/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("RIEGO"))
                .andExpect(jsonPath("$[1].tipo").value("OTRO"));
    }
}