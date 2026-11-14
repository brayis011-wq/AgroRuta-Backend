package com.agroruta.shared.security;

/** Responde si un recurso pertenece a un agricultor. */
public interface ConsultaPropiedad {

    /** false si el recurso no existe, no tiene dueño o es de otro agricultor. */
    boolean esDueno(Recurso recurso, Long recursoId, Long agricultorId);
}
