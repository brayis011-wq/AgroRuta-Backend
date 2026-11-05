package com.agroruta.crop.application.ports.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que el módulo de cultivos necesita saber de los jornales: cuánto se pagó de mano de obra
 * a una siembra. Lo implementa el módulo de trabajadores.
 */
public interface ManoObraPort {

    List<JornalDeSiembra> jornalesDeSiembra(Long siembraId);

    record JornalDeSiembra(LocalDate fecha, Long trabajadorId, String trabajador, BigDecimal valor) {}
}
