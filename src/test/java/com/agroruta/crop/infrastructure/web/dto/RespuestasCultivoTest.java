package com.agroruta.crop.infrastructure.web.dto;

import com.agroruta.crop.domain.*;
import com.agroruta.crop.infrastructure.web.dto.Respuestas.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Las respuestas de la API de cultivos deben producir exactamente el mismo JSON que producía
 * el objeto de dominio, para no romper el frontend. Si se agrega un campo al dominio, esta
 * prueba falla hasta que se decida si sale o no en la respuesta.
 */
@DisplayName("Respuestas de la API de cultivos")
class RespuestasCultivoTest {

    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final LocalDate dia = LocalDate.of(2026, 11, 7);
    private final LocalDateTime momento = LocalDateTime.of(2026, 11, 7, 10, 30);

    private void mismoJson(Object dominio, Object respuesta) {
        JsonNode esperado = json.valueToTree(dominio);
        JsonNode obtenido = json.valueToTree(respuesta);
        assertEquals(esperado, obtenido, () -> "dominio: " + esperado + "\nrespuesta: " + obtenido);
    }

    @Test
    void finca() {
        Finca f = new Finca(1L, "El Triunfo", "Pasca", 32.0, 9L);
        f.actualizarCentroide(4.3, -74.3);
        f.eliminar(momento);
        mismoJson(f, FincaResponse.from(f));
    }

    @Test
    void lote() {
        Lote l = new Lote(2L, "Lote A", 9.3, 1L);
        l.actualizarGeometria("[[1,2]]", 9.3, 4.3, -74.3);
        mismoJson(l, LoteResponse.from(l));
    }

    @Test
    void siembra() {
        Siembra s = new Siembra(3L, dia, 1000, new VariedadCultivo(5L, "Bola roja", true, 4L, "Fríjol", true), 2L);
        s.eliminar(dia, momento);
        mismoJson(s, SiembraResponse.from(s));
    }

    @Test
    void cosecha() {
        Cosecha c = new Cosecha();
        c.setId(4L); c.setFecha(dia); c.setCantidadKg(100.0); c.setCalidad(CalidadCosecha.PRIMERA);
        c.setObservaciones("buena"); c.setSiembraId(3L); c.setPrecioKg(8000.0);
        mismoJson(c, CosechaResponse.from(c));
    }

    @Test
    void fumigacion() {
        Fumigacion f = Fumigacion.fromCatalog(dia, "Glifosato", 10L, 2.5, UnidadMedida.LITROS, 5.0, "obs", 3L);
        f.setId(6L);
        f.fijarRestricciones(new RestriccionesFumigacion(2, 24, 22500.0, 0.75, "LITROS"));
        mismoJson(f, FumigacionResponse.from(f));
    }

    @Test
    void actividadCultivo() {
        ActividadCultivo a = new ActividadCultivo();
        a.setId(7L); a.setTipo(TipoActividad.PODA); a.setDescripcion("poda"); a.setFecha(dia);
        a.setSiembraId(3L); a.setCosto(50000.0);
        mismoJson(a, ActividadCultivoResponse.from(a));
    }

    @Test
    void etapaSiembra() {
        EtapaSiembra e = new EtapaSiembra(8L, 3L, EstadoCultivo.GERMINACION, dia);
        mismoJson(e, EtapaSiembraResponse.from(e));
    }

    @Test
    void catalogo() {
        VariedadCultivo v = new VariedadCultivo(5L, "Bola roja", true, 4L, "Fríjol", true);
        TipoCultivo t = new TipoCultivo(4L, "Fríjol", true, List.of(v));
        mismoJson(v, VariedadCultivoResponse.from(v));
        mismoJson(t, TipoCultivoResponse.from(t));
    }
}
