package com.agroruta.crop.application.ports.out;

import com.agroruta.crop.domain.Lluvia;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface LluviaRepository {

    List<Lluvia> deFinca(Long fincaId, LocalDate desde, LocalDate hasta);

    /** Guarda o reemplaza el dato de ese día */
    void anotar(Long fincaId, LocalDate fecha, BigDecimal milimetros);

    void borrar(Long fincaId, LocalDate fecha);
}
