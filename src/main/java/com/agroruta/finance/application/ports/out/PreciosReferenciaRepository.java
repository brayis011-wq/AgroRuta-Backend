package com.agroruta.finance.application.ports.out;

import com.agroruta.finance.domain.PrecioReferencia;
import com.agroruta.finance.domain.PreferenciaPrecio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PreciosReferenciaRepository {

    /** Guarda (o reemplaza) los precios de esas semanas */
    void guardar(List<PrecioReferencia> precios, LocalDateTime cuando);

    /** Los precios de la semana más reciente que se tiene */
    List<PrecioReferencia> ultimaSemana();

    /** Cuándo se trajeron precios por última vez */
    Optional<LocalDateTime> ultimaActualizacion();

    Optional<PreferenciaPrecio> preferencia(Long agricultorId, String cultivo);

    List<PreferenciaPrecio> preferencias(Long agricultorId);

    void guardarPreferencia(Long agricultorId, PreferenciaPrecio preferencia, LocalDateTime cuando);

    void borrarPreferencia(Long agricultorId, String cultivo);
}
