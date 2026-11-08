package com.agroruta.crop.infrastructure.web.dto;

import com.agroruta.crop.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que la API de cultivos devuelve. Antes se devolvía el objeto de dominio y cualquier campo
 * nuevo quedaba expuesto sin decidirlo; ahora cada respuesta dice exactamente qué sale.
 * Los campos (nombres y orden) son los mismos que tenía el dominio, para no cambiar el JSON
 * que usa el frontend (lo comprueba RespuestasCultivoTest).
 */
public final class Respuestas {

    private Respuestas() {}

    public record FincaResponse(Long id, String nombre, String ubicacion, Double hectareas, Long agricultorId,
                                LocalDateTime fechaRegistro, Double centroideLat, Double centroideLng,
                                LocalDateTime eliminadoEn) {
        public static FincaResponse from(Finca f) {
            return new FincaResponse(f.getId(), f.getNombre(), f.getUbicacion(), f.getHectareas(), f.getAgricultorId(),
                    f.getFechaRegistro(), f.getCentroideLat(), f.getCentroideLng(), f.getEliminadoEn());
        }
    }

    public record LoteResponse(Long id, String nombre, Double area, EstadoLote estado, Long fincaId,
                               String coordenadas, Double centroideLat, Double centroideLng, LocalDateTime eliminadoEn) {
        public static LoteResponse from(Lote l) {
            return new LoteResponse(l.getId(), l.getNombre(), l.getArea(), l.getEstado(), l.getFincaId(),
                    l.getCoordenadas(), l.getCentroideLat(), l.getCentroideLng(), l.getEliminadoEn());
        }
    }

    public record SiembraResponse(Long id, LocalDate fechaSiembra, Integer cantidadPlantas, String tipoCultivo,
                                  String variedad, Long variedadId, EstadoCultivo estadoCultivo, Long loteId,
                                  LocalDate fechaFin, LocalDateTime eliminadoEn) {
        public static SiembraResponse from(Siembra s) {
            return new SiembraResponse(s.getId(), s.getFechaSiembra(), s.getCantidadPlantas(), s.getTipoCultivo(),
                    s.getVariedad(), s.getVariedadId(), s.getEstadoCultivo(), s.getLoteId(), s.getFechaFin(),
                    s.getEliminadoEn());
        }
    }

    public record CosechaResponse(Long id, LocalDate fecha, Double cantidadKg, CalidadCosecha calidad,
                                  String observaciones, Long siembraId, Double precioKg) {
        public static CosechaResponse from(Cosecha c) {
            return new CosechaResponse(c.getId(), c.getFecha(), c.getCantidadKg(), c.getCalidad(),
                    c.getObservaciones(), c.getSiembraId(), c.getPrecioKg());
        }
    }

    public record FumigacionResponse(Long id, LocalDate fecha, String producto, Long agriculturalInputId, Double dosis,
                                     UnidadMedida unidadMedida, Double areaAplicada, String observaciones,
                                     Long siembraId, Integer diasCarencia, Integer reentradaHoras, Double costo,
                                     Double cantidadUsada, UnidadMedida unidadCantidad) {
        public static FumigacionResponse from(Fumigacion f) {
            return new FumigacionResponse(f.getId(), f.getFecha(), f.getProducto(), f.getAgriculturalInputId(),
                    f.getDosis(), f.getUnidadMedida(), f.getAreaAplicada(), f.getObservaciones(), f.getSiembraId(),
                    f.getDiasCarencia(), f.getReentradaHoras(), f.getCosto(), f.getCantidadUsada(), f.getUnidadCantidad());
        }
    }

    /** Labor registrada en una siembra (poda, riego…), no confundir con el catálogo de labores de los trabajadores */
    public record ActividadCultivoResponse(Long id, TipoActividad tipo, String descripcion, LocalDate fecha,
                                           Long siembraId, Double costo) {
        public static ActividadCultivoResponse from(ActividadCultivo a) {
            return new ActividadCultivoResponse(a.getId(), a.getTipo(), a.getDescripcion(), a.getFecha(),
                    a.getSiembraId(), a.getCosto());
        }
    }

    public record EtapaSiembraResponse(Long id, Long siembraId, EstadoCultivo etapa, LocalDate fecha) {
        public static EtapaSiembraResponse from(EtapaSiembra e) {
            return new EtapaSiembraResponse(e.getId(), e.getSiembraId(), e.getEtapa(), e.getFecha());
        }
    }

    public record VariedadCultivoResponse(Long id, String nombre, boolean activo, Long tipoCultivoId,
                                          String tipoCultivoNombre, boolean tipoCultivoActivo) {
        public static VariedadCultivoResponse from(VariedadCultivo v) {
            return new VariedadCultivoResponse(v.getId(), v.getNombre(), v.isActivo(), v.getTipoCultivoId(),
                    v.getTipoCultivoNombre(), v.isTipoCultivoActivo());
        }
    }

    public record TipoCultivoResponse(Long id, String nombre, boolean activo, List<VariedadCultivoResponse> variedades) {
        public static TipoCultivoResponse from(TipoCultivo t) {
            return new TipoCultivoResponse(t.getId(), t.getNombre(), t.isActivo(),
                    t.getVariedades() == null ? null : t.getVariedades().stream().map(VariedadCultivoResponse::from).toList());
        }
    }
}
