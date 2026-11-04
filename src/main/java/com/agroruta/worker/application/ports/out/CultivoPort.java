package com.agroruta.worker.application.ports.out;

import java.util.List;
import java.util.Optional;

/**
 * Lo que el módulo de trabajadores necesita saber de los cultivos (siembras).
 * Lo implementa el módulo crop; worker no conoce fincas, lotes ni siembras.
 */
public interface CultivoPort {

    /** El cultivo (siembra) con ese id, si existe. */
    Optional<CultivoInfo> buscar(Long cultivoId);

    /** Cultivos que siguen activos (no finalizados) en las fincas del agricultor. */
    List<CultivoInfo> activosDelAgricultor(Long agricultorId);

    record CultivoInfo(Long id, Long agricultorId,
                       Long fincaId, String fincaNombre,
                       Long loteId, String loteNombre,
                       String tipoCultivo, String variedad, String estado) {

        public boolean finalizado() {
            return "FINALIZADO".equals(estado);
        }

        /** Nombre que se guarda en el jornal, p. ej. "Lote 2 · La Esperanza". */
        public String nombre() {
            return loteNombre + " · " + fincaNombre;
        }
    }
}
