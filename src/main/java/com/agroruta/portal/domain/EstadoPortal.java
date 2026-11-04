package com.agroruta.portal.domain;

import com.agroruta.worker.domain.Trabajador;

/**
 * Lo que ve un trabajador al entrar: si su cuenta está vinculada y a quién.
 *
 * @param estado      SIN_SOLICITUD, PENDIENTE, APROBADO o RECHAZADO
 * @param trabajador  sus datos como trabajador (solo cuando está aprobado)
 * @param empleador   nombre del agricultor (solo cuando está aprobado)
 */
public record EstadoPortal(String estado, Trabajador trabajador, String empleador) {

    public static final String SIN_SOLICITUD = "SIN_SOLICITUD";
}
