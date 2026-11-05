package com.agroruta.notification.application.ports.out;

import com.agroruta.notification.domain.Correo;

public interface CorreoPort {
    /** Envía el correo. No lanza excepciones: si falla, lo deja en el log. Devuelve si salió. */
    boolean enviar(Correo correo);
}
