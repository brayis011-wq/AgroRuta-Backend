package com.agroruta.finance.infrastructure.sipsa;

import com.agroruta.finance.application.ports.in.PreciosReferenciaUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Al arrancar (cada despliegue), si los precios de SIPSA faltan o están viejos se traen en segundo plano:
 * así ya están cuando alguien abre la pantalla, sin esperar a la tarea diaria. Las pruebas lo apagan
 * (sipsa.al-arrancar=false) para no salir a internet.
 */
@Component
public class PreciosAlArrancar {

    private final PreciosReferenciaUseCase precios;
    private final boolean activo;

    public PreciosAlArrancar(PreciosReferenciaUseCase precios, @Value("${sipsa.al-arrancar:true}") boolean activo) {
        this.precios = precios;
        this.activo = activo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        if (activo) precios.actualizarSiHaceFalta();
    }
}
