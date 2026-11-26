package com.agroruta.crop.application.ports.in;

import com.agroruta.crop.domain.DiagnosticoPlaga;
import com.agroruta.crop.domain.DiagnosticosSiembra;

/** Diagnóstico de plagas y enfermedades a partir de una foto, guardado en la bitácora de la siembra. */
public interface DiagnosticoPlagaUseCase {

    DiagnosticosSiembra deSiembra(Long siembraId, Long usuarioId);

    /**
     * Analiza la foto y guarda el resultado.
     *
     * @param archivo la foto tal como la subió la persona (JPG o PNG)
     * @param nota    lo que la persona quiso contar sobre lo que ve (opcional)
     */
    DiagnosticoPlaga diagnosticar(Long siembraId, Long usuarioId, byte[] archivo, String nota);

    byte[] foto(Long siembraId, Long diagnosticoId);

    void eliminar(Long siembraId, Long diagnosticoId);
}
