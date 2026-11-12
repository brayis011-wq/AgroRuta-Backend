package com.agroruta.crop.domain;

import java.util.List;

/**
 * Los diagnósticos de una siembra y si se pueden pedir más.
 *
 * @param disponible   false si el servidor no tiene configurado el análisis de fotos
 * @param restantesHoy cuántas fotos más puede analizar hoy quien consulta
 */
public record DiagnosticosSiembra(boolean disponible, int restantesHoy, List<DiagnosticoPlaga> diagnosticos) {
}
