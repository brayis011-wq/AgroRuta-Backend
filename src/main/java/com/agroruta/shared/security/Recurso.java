package com.agroruta.shared.security;

/** Recursos que pertenecen a un agricultor, directamente o a través de su finca o de su trabajador. */
public enum Recurso {
    FINCA,
    LOTE,
    SIEMBRA,
    COSECHA,
    FUMIGACION,
    ACTIVIDAD_CULTIVO,
    LABOR,
    TRABAJADOR,
    CARGO,
    JORNAL,
    NOMINA,
    PAGO,
    /** Gasto general de una finca */
    GASTO_FINCA,
    /** Venta a un comprador (cuentas por cobrar) */
    VENTA,
    /** Actividad que se repite en una siembra */
    ACTIVIDAD_PROGRAMADA
}
