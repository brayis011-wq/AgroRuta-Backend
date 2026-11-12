package com.agroruta.user.application.ports.in;

import com.agroruta.user.domain.SesionAbierta;

import java.util.List;

/**
 * "Sesiones abiertas": ver en qué dispositivos está iniciada la sesión y cerrarlas a distancia.
 * Quien pregunta se identifica con el token de su cookie de sesión; si no sirve,
 * se lanza SesionInvalidaException.
 */
public interface SesionesAbiertasUseCase {

    /** Las sesiones abiertas del dueño del token: primero la actual, luego las más recientes. */
    List<SesionAbierta> listar(String token);

    /** Cierra una de sus sesiones. Devuelve true si cerró la sesión desde la que pregunta. */
    boolean cerrar(String token, Long sesionId);

    /** Cierra todas menos la actual. Devuelve cuántas cerró. */
    int cerrarOtras(String token);
}
