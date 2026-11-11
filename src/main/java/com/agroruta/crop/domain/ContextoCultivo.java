package com.agroruta.crop.domain;

/** Lo que se sabe de la siembra y ayuda a interpretar la foto. */
public record ContextoCultivo(String cultivo, String variedad, String etapa, Long diasDesdeSiembra, String nota) {
}
