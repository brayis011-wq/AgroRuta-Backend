package com.agroruta.user.infrastructure.web;

import com.agroruta.user.application.ports.out.DispositivoPort;
import com.agroruta.user.domain.Dispositivo;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Lee el User-Agent de la petición en curso. Fuera de una petición (tareas, pruebas) responde null. */
@Component
public class DispositivoDeLaPeticion implements DispositivoPort {

    @Override
    public String actual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a) {
            return Dispositivo.describir(a.getRequest().getHeader(HttpHeaders.USER_AGENT));
        }
        return null;
    }
}
