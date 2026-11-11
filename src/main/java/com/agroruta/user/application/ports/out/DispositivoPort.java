package com.agroruta.user.application.ports.out;

/** Desde qué dispositivo llega la petición que se está atendiendo ("Chrome · Windows"); null si no se sabe. */
public interface DispositivoPort {
    String actual();
}
