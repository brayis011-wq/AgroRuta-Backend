package com.agroruta.crop.domain;

import java.util.List;
import java.util.Optional;

public interface CatalogoCultivoRepository {
    /** Cultivos con sus variedades, ordenados por nombre. */
    List<TipoCultivo> listarTipos(boolean soloActivos);
    Optional<TipoCultivo> buscarTipo(Long id);
    Optional<VariedadCultivo> buscarVariedad(Long id);
    boolean existeTipo(String nombre);
    boolean existeVariedad(Long tipoCultivoId, String nombre);
    TipoCultivo guardarTipo(TipoCultivo tipo);
    VariedadCultivo guardarVariedad(VariedadCultivo variedad);
    long contarTipos();
}
