package com.agroruta.inventario.domain;

public enum EstadoStock {
    /** Hay producto (y está por encima del mínimo, si se definió uno) */
    OK,
    /** Queda producto, pero igual o menos que el mínimo definido */
    BAJO,
    /** No queda nada */
    AGOTADO,
    /** Se gastó más de lo que se registró como comprado: falta registrar una compra o hacer un ajuste */
    NEGATIVO;

    public boolean requiereAtencion() {
        return this != OK;
    }
}
