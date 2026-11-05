package com.agroruta.notification.application;

import com.agroruta.notification.application.ports.out.CorreoPort;
import com.agroruta.notification.domain.Correo;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Envía en segundo plano, para que la respuesta al usuario no espere a Gmail
 * (y para que "olvidé mi contraseña" tarde lo mismo exista o no la cuenta).
 */
@Component
public class EnvioCorreo {

    private final CorreoPort correoPort;

    public EnvioCorreo(CorreoPort correoPort) {
        this.correoPort = correoPort;
    }

    @Async("correos")
    public void enSegundoPlano(Correo correo) {
        correoPort.enviar(correo);
    }
}
