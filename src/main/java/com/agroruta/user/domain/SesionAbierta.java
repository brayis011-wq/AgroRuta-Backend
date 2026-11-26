package com.agroruta.user.domain;

import java.time.Instant;

/**
 * Una sesión abierta, como se le muestra a la persona.
 *
 * @param id          id de la sesión (sirve para cerrarla)
 * @param dispositivo "Chrome · Windows" (null si no se supo)
 * @param ultimoUso   última vez que ese dispositivo renovó la sesión
 * @param actual      es la sesión desde la que se está preguntando
 */
public record SesionAbierta(Long id, String dispositivo, Instant iniciadaEn, Instant ultimoUso, boolean actual) {}
