package com.agroruta.shared.evento;

/** Una cuenta de trabajador pidió ver los datos de este trabajador; el agricultor debe aprobarla. */
public record AccesoPortalSolicitado(Long trabajadorId) {}
