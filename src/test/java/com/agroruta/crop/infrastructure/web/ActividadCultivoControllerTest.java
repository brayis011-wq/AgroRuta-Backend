package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.ports.in.ActividadCultivoUseCase;
import com.agroruta.crop.domain.ActividadCultivo;
import com.agroruta.crop.domain.TipoActividad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActividadCultivoControllerTest {

    @Mock
    private ActividadCultivoUseCase actividadUseCase;

    @InjectMocks
    private ActividadCultivoController controller;

    private ActividadCultivo actividadMock;

    @BeforeEach
    void setUp() {
        actividadMock = new ActividadCultivo(
                1L, TipoActividad.RIEGO, "Riego inicial", LocalDate.of(2026, 4, 1), 10L
        );
    }

    @Test
    void registrar_retorna201() {
        when(actividadUseCase.registrarActividad(any(), any(), any(), any()))
                .thenReturn(actividadMock);

        var req = new com.agroruta.crop.infrastructure.web.dto.ActividadRequest();
        req.setTipo("RIEGO");
        req.setDescripcion("Riego inicial");
        req.setFecha(LocalDate.of(2026, 4, 1));
        req.setSiembraId(10L);

        ResponseEntity<ActividadCultivo> response = controller.registrar(req);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDescripcion()).isEqualTo("Riego inicial");
    }

    @Test
    void listarPorSiembra_retorna200() {
        when(actividadUseCase.listarActividadesPorSiembra(10L))
                .thenReturn(List.of(actividadMock));

        ResponseEntity<List<ActividadCultivo>> response = controller.listarPorSiembra(10L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
    }
}