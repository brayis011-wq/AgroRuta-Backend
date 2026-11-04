package com.agroruta.crop.application.ports.in;

import com.agroruta.crop.domain.TipoCultivo;
import com.agroruta.crop.domain.VariedadCultivo;

import java.util.List;

public interface CatalogoCultivoUseCase {
    List<TipoCultivo> listar(boolean soloActivos);
    TipoCultivo crearTipo(String nombre);
    VariedadCultivo crearVariedad(Long tipoCultivoId, String nombre);
    TipoCultivo cambiarEstadoTipo(Long id, boolean activo);
    VariedadCultivo cambiarEstadoVariedad(Long id, boolean activo);
}
