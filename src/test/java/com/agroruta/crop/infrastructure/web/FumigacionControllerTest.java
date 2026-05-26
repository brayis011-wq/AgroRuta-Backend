package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.ports.in.FumigacionUseCase;
import com.agroruta.crop.domain.Fumigacion;
import com.agroruta.crop.domain.UnidadMedida;
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
class FumigacionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FumigacionUseCase fumigacionUseCase;

    @InjectMocks
    private FumigacionController controller;

    private final LocalDate FECHA = LocalDate.of(2025, 3, 10);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  POST /api/fumigaciones
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaRegistrarFumigacionDesdeCatalogoYRetornar201() throws Exception {
        Fumigacion fumigacion = Fumigacion.fromCatalog(
                FECHA, "Roundup", 7L, 2.5, UnidadMedida.LITROS, 5.0, "Aplicación preventiva", 10L
        );
        fumigacion.setId(1L);

        when(fumigacionUseCase.registrarFumigacion(
                any(), anyString(), eq(7L), anyDouble(), anyString(), anyDouble(), anyString(), anyLong()
        )).thenReturn(fumigacion);

        String body = """
                {
                    "fecha": "%s",
                    "producto": "Roundup",
                    "agriculturalInputId": 7,
                    "dosis": 2.5,
                    "unidadMedida": "LITROS",
                    "areaAplicada": 5.0,
                    "observaciones": "Aplicación preventiva",
                    "siembraId": 10
                }
                """.formatted(FECHA);

        mockMvc.perform(post("/api/fumigaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.producto").value("Roundup"))
                .andExpect(jsonPath("$.agriculturalInputId").value(7))
                .andExpect(jsonPath("$.dosis").value(2.5))
                .andExpect(jsonPath("$.unidadMedida").value("LITROS"))
                .andExpect(jsonPath("$.areaAplicada").value(5.0))
                .andExpect(jsonPath("$.siembraId").value(10));
    }

    @Test
    void deberiaRegistrarFumigacionManualSinInsumoDelCatalogoYRetornar201() throws Exception {
        Fumigacion fumigacion = Fumigacion.fromManualEntry(
                FECHA, "Producto casero", 1.0, UnidadMedida.KG, 3.0, "Mezcla artesanal", 10L
        );
        fumigacion.setId(2L);

        // agriculturalInputId es null → se usa isNull() en lugar de anyLong()
        when(fumigacionUseCase.registrarFumigacion(
                any(), anyString(), isNull(), anyDouble(), anyString(), anyDouble(), anyString(), anyLong()
        )).thenReturn(fumigacion);

        String body = """
                {
                    "fecha": "%s",
                    "producto": "Producto casero",
                    "agriculturalInputId": null,
                    "dosis": 1.0,
                    "unidadMedida": "KG",
                    "areaAplicada": 3.0,
                    "observaciones": "Mezcla artesanal",
                    "siembraId": 10
                }
                """.formatted(FECHA);

        mockMvc.perform(post("/api/fumigaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.producto").value("Producto casero"))
                .andExpect(jsonPath("$.agriculturalInputId").doesNotExist())
                .andExpect(jsonPath("$.dosis").value(1.0))
                .andExpect(jsonPath("$.unidadMedida").value("KG"))
                .andExpect(jsonPath("$.siembraId").value(10));
    }

    @Test
    void deberiaRegistrarFumigacionConUnidadML() throws Exception {
        Fumigacion fumigacion = Fumigacion.fromCatalog(
                FECHA, "Clorpirifos", 3L, 500.0, UnidadMedida.ML, 2.0, null, 10L
        );
        fumigacion.setId(3L);

        when(fumigacionUseCase.registrarFumigacion(
                any(), anyString(), eq(3L), anyDouble(), anyString(), anyDouble(), isNull(), anyLong()
        )).thenReturn(fumigacion);

        String body = """
                {
                    "fecha": "%s",
                    "producto": "Clorpirifos",
                    "agriculturalInputId": 3,
                    "dosis": 500.0,
                    "unidadMedida": "ML",
                    "areaAplicada": 2.0,
                    "observaciones": null,
                    "siembraId": 10
                }
                """.formatted(FECHA);

        mockMvc.perform(post("/api/fumigaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.unidadMedida").value("ML"))
                .andExpect(jsonPath("$.dosis").value(500.0))
                .andExpect(jsonPath("$.agriculturalInputId").value(3));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  GET /api/fumigaciones/siembra/{siembraId}
    // ──────────────────────────────────────────────────────────────────────

    @Test
    void deberiaListarFumigacionesPorSiembra() throws Exception {
        Fumigacion f1 = Fumigacion.fromCatalog(
                FECHA, "Roundup", 7L, 2.5, UnidadMedida.LITROS, 5.0, "Preventiva", 10L
        );
        f1.setId(1L);

        Fumigacion f2 = Fumigacion.fromManualEntry(
                FECHA.plusDays(7), "Producto casero", 1.0, UnidadMedida.KG, 3.0, "Correctiva", 10L
        );
        f2.setId(2L);

        when(fumigacionUseCase.listarFumigacionesPorSiembra(10L)).thenReturn(List.of(f1, f2));

        mockMvc.perform(get("/api/fumigaciones/siembra/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].producto").value("Roundup"))
                .andExpect(jsonPath("$[0].agriculturalInputId").value(7))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].producto").value("Producto casero"))
                .andExpect(jsonPath("$[1].agriculturalInputId").doesNotExist());
    }

    @Test
    void deberiaRetornarListaVaciaCuandoSiembraSinFumigaciones() throws Exception {
        when(fumigacionUseCase.listarFumigacionesPorSiembra(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/fumigaciones/siembra/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}