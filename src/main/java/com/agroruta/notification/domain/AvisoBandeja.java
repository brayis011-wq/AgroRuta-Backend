package com.agroruta.notification.domain;

import java.time.Instant;
import java.util.Map;

/**
 * Un aviso de la bandeja de la app (la campana).
 *
 * @param tipo   qué pasó; el frontend arma el texto en el idioma de la persona con {@code datos}
 * @param titulo texto en español por si el frontend no conoce el tipo
 * @param ruta   pantalla que se abre al tocarlo
 */
public record AvisoBandeja(Long id, Tipo tipo, String titulo, String cuerpo, Map<String, Object> datos,
                           String ruta, Instant creadoEn, Instant leidoEn) {

    public enum Tipo {
        /** Al trabajador: le registraron un pago */
        PAGO_REGISTRADO,
        /** Al agricultor: un trabajador pidió entrar a su portal */
        ACCESO_SOLICITADO,
        /** Al trabajador: aprobaron, rechazaron o retiraron su acceso */
        ACCESO_RESUELTO,
        /** Al agricultor: lo de hoy en sus cultivos (labores, clima, insumos) */
        RESUMEN_DIA
    }

    public boolean leido() {
        return leidoEn != null;
    }

    /** Lo que se guarda: un aviso nuevo, todavía sin id ni fecha */
    public record Nuevo(Long usuarioId, Tipo tipo, String titulo, String cuerpo, Map<String, Object> datos,
                        String ruta, String etiqueta) {}
}
