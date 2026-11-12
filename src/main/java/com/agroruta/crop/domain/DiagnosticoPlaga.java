package com.agroruta.crop.domain;

import java.time.LocalDateTime;
import java.util.List;

/** Un diagnóstico guardado en la bitácora de la siembra (la foto se pide aparte). */
public record DiagnosticoPlaga(Long id,
                               Long siembraId,
                               LocalDateTime fecha,
                               String nota,
                               String problema,
                               TipoProblema tipo,
                               ConfianzaDiagnostico confianza,
                               String sintomas,
                               List<String> recomendaciones,
                               List<String> otrasPosibilidades,
                               String advertencia) {
}
