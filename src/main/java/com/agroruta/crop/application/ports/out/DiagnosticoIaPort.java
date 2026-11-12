package com.agroruta.crop.application.ports.out;

import com.agroruta.crop.domain.AnalisisFoto;
import com.agroruta.crop.domain.ContextoCultivo;

import java.util.Optional;

/** Servicio que mira la foto de un cultivo y dice qué problema puede tener. */
public interface DiagnosticoIaPort {

    /** false si el servidor no tiene con qué analizar fotos (falta la clave del servicio) */
    boolean disponible();

    /**
     * @param jpeg foto ya reducida y sin metadatos
     * @return vacío si el servicio declinó analizar la foto
     * @throws com.agroruta.shared.exception.ExternalApiException si el servicio falló o no respondió
     */
    Optional<AnalisisFoto> analizar(byte[] jpeg, ContextoCultivo contexto);
}
