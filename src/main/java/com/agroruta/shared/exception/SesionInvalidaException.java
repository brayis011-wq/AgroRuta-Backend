package com.agroruta.shared.exception;

/** La cookie de sesión no existe, venció o fue revocada: hay que volver a iniciar sesión (401). */
public class SesionInvalidaException extends RuntimeException {
    public SesionInvalidaException(String message) {
        super(message);
    }
}
