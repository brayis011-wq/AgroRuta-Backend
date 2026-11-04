package com.agroruta.worker.domain;

/** Estado del acceso de un trabajador a su portal (sus jornales, nóminas y pagos). */
public enum EstadoAccesoPortal {
    /** Se registró con esta cédula y espera que el agricultor lo apruebe. */
    PENDIENTE,
    /** El agricultor confirmó que la cuenta es de este trabajador. */
    APROBADO,
    /** El agricultor no reconoció la solicitud o retiró el acceso. */
    RECHAZADO
}
