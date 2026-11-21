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
    public List<LaborResponse> actividades;
    public String observaciones;
    public BigDecimal valorJornal;
    public boolean liquidado;
    /** Solo en los jornales pagados por kilo */
    public BigDecimal kilos;
    public BigDecimal valorKilo;

    public static JornalResponse from(Jornal j) {
        JornalResponse r = new JornalResponse();
        r.id = j.getId();
        r.fecha = j.getFecha();
        r.trabajador = TrabajadorResponse.from(j.getTrabajador());
        r.cultivoId = j.getCultivoId();
        r.nombreCultivo = j.getNombreCultivo();
        r.actividades = j.getActividades().stream()
                .map(LaborResponse::from)
                .toList();
        r.observaciones = j.getObservaciones();
        r.valorJornal = j.getValorJornal();
        r.liquidado = j.isLiquidado();
        r.kilos = j.getKilos();
        r.valorKilo = j.getValorKilo();
        return r;
    }
}