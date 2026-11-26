package com.agroruta.worker.domain;

/** Cómo se le paga a quien tiene el cargo. */
public enum ModalidadPago {
    /** Un valor fijo por día trabajado */
    JORNAL,
    /** Los kilos recogidos ese día por el valor del kilo (recolección de café, mora...) */
    KILO
}
