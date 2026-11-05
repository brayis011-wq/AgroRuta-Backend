package com.agroruta.notification.infrastructure.correo;

import com.agroruta.notification.application.ports.out.CorreoPort;
import com.agroruta.notification.domain.Correo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cuando Gmail no está configurado (desarrollo local): no envía nada, deja el correo en el log.
 * Con correo.log-contenido=true también escribe el texto (por ejemplo, el enlace para restablecer).
 */
public class LogCorreoAdapter implements CorreoPort {

    private static final Logger log = LoggerFactory.getLogger(LogCorreoAdapter.class);

    private final boolean mostrarContenido;

    public LogCorreoAdapter(boolean mostrarContenido) {
        this.mostrarContenido = mostrarContenido;
    }

    @Override
    public boolean enviar(Correo correo) {
        if (mostrarContenido) {
            log.info("Correo sin enviar (Gmail no configurado) — \"{}\"\n{}", correo.asunto(), correo.texto());
        } else {
            log.info("Correo sin enviar (Gmail no configurado) — \"{}\"", correo.asunto());
        }
        return false;
    }
}
