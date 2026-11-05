package com.agroruta.shared.evento;

/** Un correo quedó bloqueado por demasiadas contraseñas equivocadas seguidas. */
public record CuentaBloqueada(String email, long minutos) {}
