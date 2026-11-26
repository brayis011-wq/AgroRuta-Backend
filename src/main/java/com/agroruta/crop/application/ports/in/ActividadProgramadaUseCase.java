package com.agroruta.crop.application.ports.in;

import com.agroruta.crop.domain.ActividadProgramada;
import com.agroruta.crop.domain.ProgramacionActividades;
import java.time.LocalDate;
import java.util.List;

/** Labores que se repiten cada N días en una siembra (aparecen en el calendario y en los avisos). */
public interface ActividadProgramadaUseCase {

    /** Una programación con su próxima fecha */
    record ConEstado(ActividadProgramada programada, ProgramacionActividades.Estado estado) {}

    ActividadProgramada crear(
            Long siembraId, String tipo, String descripcion, Integer cadaDias, LocalDate desde, LocalDate hasta);

    ActividadProgramada actualizar(
            Long id, String tipo, String descripcion, Integer cadaDias, LocalDate desde, LocalDate hasta);

    void eliminar(Long id);

    List<ConEstado> deSiembra(Long siembraId);
}
