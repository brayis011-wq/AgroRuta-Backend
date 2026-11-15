package com.agroruta.shared.exception;

/**
 * Un dato que mandó el usuario no sirve: falta, está mal escrito o no es una de las opciones.
 * Responde 400 con el código AGR-020, igual que antes lo hacía IllegalArgumentException.
 */
public class DatoInvalidoException extends BusinessException {

    public DatoInvalidoException(String message) {
        super(ErrorCode.INVALID_ARGUMENT, message);
    }
}
