package com.agroruta.inventario.domain;

/** Qué le pasó al stock de un insumo. */
public enum TipoMovimiento {
    /** Entró producto comprado (suma) */
    COMPRA,
    /** Se gastó en una fumigación (resta); lo maneja la fumigación, no se edita a mano */
    USO,
    /** Corrección por conteo físico (suma o resta la diferencia) */
    AJUSTE
}
