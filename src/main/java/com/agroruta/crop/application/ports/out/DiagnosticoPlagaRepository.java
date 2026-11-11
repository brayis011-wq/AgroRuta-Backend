package com.agroruta.crop.application.ports.out;

import com.agroruta.crop.domain.AnalisisFoto;
import com.agroruta.crop.domain.DiagnosticoPlaga;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiagnosticoPlagaRepository {

    DiagnosticoPlaga guardar(Long siembraId, Long usuarioId, LocalDateTime fecha, byte[] foto, String nota, AnalisisFoto analisis);

    /** Los más recientes primero */
    List<DiagnosticoPlaga> deSiembra(Long siembraId, int limite);

    Optional<byte[]> foto(Long siembraId, Long diagnosticoId);

    /** @return false si ese diagnóstico no existe en esa siembra */
    boolean eliminar(Long siembraId, Long diagnosticoId);

    // ── Tope diario de fotos analizadas ──

    void anotarUso(Long usuarioId, LocalDate dia);

    /** Devuelve un uso que no llegó a gastarse (el servicio falló) */
    void devolverUso(Long usuarioId, LocalDate dia);

    int usoDe(Long usuarioId, LocalDate dia);

    /** Fotos analizadas ese día entre todas las cuentas */
    int usoTotal(LocalDate dia);
}
