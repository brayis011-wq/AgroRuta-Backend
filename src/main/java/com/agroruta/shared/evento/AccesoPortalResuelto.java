package com.agroruta.shared.evento;

/**
 * El agricultor aprobó o rechazó la solicitud de acceso al portal.
 * retirado = el acceso ya estaba aprobado y se quitó.
 */
public record AccesoPortalResuelto(Long trabajadorId, boolean aprobado, boolean retirado) {}
