package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.in.CatalogoCultivoUseCase;
import com.agroruta.crop.application.ports.out.CatalogoCultivoRepository;
import com.agroruta.crop.domain.TipoCultivo;
import com.agroruta.crop.domain.VariedadCultivo;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Catálogo de cultivos y variedades. Lo administra el Administrador;
 * los agricultores lo usan al registrar una siembra.
 * No se borra nada: se desactiva, para no romper las siembras ya registradas.
 */
@Service
public class CatalogoCultivoService implements CatalogoCultivoUseCase {

    private final CatalogoCultivoRepository catalogo;

    public CatalogoCultivoService(CatalogoCultivoRepository catalogo) {
        this.catalogo = catalogo;
    }

    @Override
    public List<TipoCultivo> listar(boolean soloActivos) {
        return catalogo.listarTipos(soloActivos);
    }

    @Override
    public TipoCultivo crearTipo(String nombre) {
        String limpio = limpiar(nombre);
        if (catalogo.existeTipo(limpio)) {
            throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Ya existe el cultivo " + limpio + ".");
        }
        return catalogo.guardarTipo(new TipoCultivo(null, limpio, true, new ArrayList<>()));
    }

    @Override
    public VariedadCultivo crearVariedad(Long tipoCultivoId, String nombre) {
        TipoCultivo tipo = catalogo.buscarTipo(tipoCultivoId)
                .orElseThrow(() -> new ResourceNotFoundException("Cultivo no encontrado con ID: " + tipoCultivoId));
        String limpio = limpiar(nombre);
        if (catalogo.existeVariedad(tipo.getId(), limpio)) {
            throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS,
                    tipo.getNombre() + " ya tiene la variedad " + limpio + ".");
        }
        return catalogo.guardarVariedad(new VariedadCultivo(null, limpio, true, tipo.getId(), tipo.getNombre(), tipo.isActivo()));
    }

    @Override
    public TipoCultivo cambiarEstadoTipo(Long id, boolean activo) {
        TipoCultivo tipo = catalogo.buscarTipo(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cultivo no encontrado con ID: " + id));
        tipo.setActivo(activo);
        return catalogo.guardarTipo(tipo);
    }

    @Override
    public VariedadCultivo cambiarEstadoVariedad(Long id, boolean activo) {
        VariedadCultivo v = catalogo.buscarVariedad(id)
                .orElseThrow(() -> new ResourceNotFoundException("Variedad no encontrada con ID: " + id));
        v.setActivo(activo);
        return catalogo.guardarVariedad(v);
    }

    /** Quita espacios de más y pone la primera letra en mayúscula: "  café " → "Café" */
    static String limpiar(String nombre) {
        String t = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (t.isEmpty()) throw new IllegalArgumentException("El nombre es obligatorio.");
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }
}
