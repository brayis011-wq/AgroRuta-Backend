package com.agroruta.user.application.ports.in;

/** Lo que llega para crear una cuenta; ya validado en la web (formato y longitudes). */
public record DatosRegistro(String nombre, String email, String password, String rol, String cedula) {}
