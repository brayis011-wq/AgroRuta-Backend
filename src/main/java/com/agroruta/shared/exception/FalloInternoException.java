package com.agroruta.shared.exception;

/**
 * Fallo interno del servidor (cifrado, firmas, PDF, QR, respuestas inesperadas de un servicio externo).
 * No es culpa de la persona: el manejador global responde 500 con un mensaje genérico y deja el detalle
 * solo en el log. Para reglas de negocio está {@link BusinessException}.
 */
public class FalloInternoException extends IllegalStateException {

    public FalloInternoException(String mensaje) {
        super(mensaje);
    }

    public FalloInternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public FalloInternoException(Throwable causa) {
        super(causa);
    }
}
