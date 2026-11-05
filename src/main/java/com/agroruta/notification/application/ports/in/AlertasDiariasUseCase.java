package com.agroruta.notification.application.ports.in;

public interface AlertasDiariasUseCase {
    /**
     * Manda el resumen del día a cada agricultor que tenga algo pendiente.
     * Si ya se mandó hoy no repite, salvo que se pida con forzar.
     * @return cuántos correos salieron
     */
    int enviarResumenDelDia(boolean forzar);
}
