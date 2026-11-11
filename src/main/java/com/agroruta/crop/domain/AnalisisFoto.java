package com.agroruta.crop.domain;

import java.util.List;

/**
 * Lo que responde el análisis de una foto, antes de guardarlo en la bitácora.
 *
 * @param esPlanta false si en la foto no se distingue un cultivo (no se guarda)
 * @param modelo   modelo que respondió, para saber con qué se hizo cada diagnóstico
 */
public record AnalisisFoto(boolean esPlanta,
                           String problema,
                           TipoProblema tipo,
                           ConfianzaDiagnostico confianza,
                           String sintomas,
                           List<String> recomendaciones,
                           List<String> otrasPosibilidades,
                           String advertencia,
                           String modelo) {
}
