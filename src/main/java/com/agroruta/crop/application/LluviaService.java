package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.out.LluviaRepository;
import com.agroruta.crop.domain.Lluvia;
import com.agroruta.shared.exception.DatoInvalidoException;
import com.agroruta.shared.util.ZonaHoraria;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lluvia medida en el pluviómetro de cada finca. */
@Service
@Transactional
public class LluviaService {

    static final ZoneId ZONA = ZonaHoraria.COLOMBIA;
    static final BigDecimal MAX_MM = new BigDecimal("500");
    static final long MAX_DIAS = 400;

    private final LluviaRepository repositorio;

    public LluviaService(LluviaRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Los días con dato entre dos fechas (por defecto, los últimos 30) y lo que suman */
    @Transactional(readOnly = true)
    public Lluvia.Resumen deFinca(Long fincaId, LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(ZONA);
        LocalDate h = hasta == null ? hoy : hasta;
        LocalDate d = desde == null ? h.minusDays(29) : desde;
        if (h.isBefore(d)) throw new DatoInvalidoException("La fecha final no puede ser anterior a la inicial.");
        if (ChronoUnit.DAYS.between(d, h) > MAX_DIAS)
            throw new DatoInvalidoException("El rango no puede superar 400 días.");
        // Para los totales de 7 y 30 días siempre se miran los últimos 30, aunque se pida otro rango
        LocalDate leerDesde = d.isBefore(hoy.minusDays(29)) ? d : hoy.minusDays(29);
        LocalDate leerHasta = h.isAfter(hoy) ? h : hoy;
        var todos = repositorio.deFinca(fincaId, leerDesde, leerHasta);
        Lluvia.Resumen r = Lluvia.resumir(todos, hoy);
        return new Lluvia.Resumen(
                r.ultimos7(),
                r.ultimos30(),
                r.ultimaLluvia(),
                r.diasSinLluvia(),
                r.dias().stream()
                        .filter(l -> !l.fecha().isBefore(d) && !l.fecha().isAfter(h))
                        .toList());
    }

    public Lluvia anotar(Long fincaId, LocalDate fecha, BigDecimal milimetros) {
        if (fecha == null) throw new DatoInvalidoException("Indica el día.");
        if (fecha.isAfter(LocalDate.now(ZONA)))
            throw new DatoInvalidoException("No se puede anotar la lluvia de un día que no ha pasado.");
        if (milimetros == null || milimetros.signum() < 0)
            throw new DatoInvalidoException("Los milímetros no pueden ser negativos.");
        if (milimetros.compareTo(MAX_MM) > 0) throw new DatoInvalidoException("Son demasiados milímetros para un día.");
        BigDecimal mm = milimetros.setScale(1, RoundingMode.HALF_UP);
        repositorio.anotar(fincaId, fecha, mm);
        return new Lluvia(fecha, mm);
    }

    public void borrar(Long fincaId, LocalDate fecha) {
        repositorio.borrar(fincaId, fecha);
    }
}
