package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.Jornal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class JornalResponse {
    public Long id;
    public LocalDate fecha;
    public TrabajadorResponse trabajador;
    public Long cultivoId;
    public String nombreCultivo;
    public List<ActividadResponse> actividades;
    public String observaciones;
    public BigDecimal valorJornal;
    public boolean liquidado;

    public static JornalResponse from(Jornal j) {
        JornalResponse r = new JornalResponse();
        r.id = j.getId();
        r.fecha = j.getFecha();
        r.trabajador = TrabajadorResponse.from(j.getTrabajador());
        r.cultivoId = j.getCultivoId();
        r.nombreCultivo = j.getNombreCultivo();
        r.actividades = j.getActividades().stream()
                .map(ActividadResponse::from)
                .toList();
        r.observaciones = j.getObservaciones();
        r.valorJornal = j.getValorJornal();
        r.liquidado = j.isLiquidado();
        return r;
    }
}