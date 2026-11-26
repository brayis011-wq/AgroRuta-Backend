package com.agroruta.shared.exception;

/**
 * La operación no cabe en el estado en que está el registro (aprobar una nómina ya pagada,
 * ocupar un lote que ya tiene cultivo). Responde 409 con el código AGR-011, igual que antes
 * lo hacía IllegalStateException.
 */
public class EstadoInvalidoException extends BusinessException {

    public EstadoInvalidoException(String message) {
        super(ErrorCode.INVALID_OPERATION, message);
    }
}
