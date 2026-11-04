package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.domain.CatalogoCultivoRepository;
import com.agroruta.crop.domain.TipoCultivo;
import com.agroruta.crop.domain.VariedadCultivo;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class CatalogoCultivoRepositoryImpl implements CatalogoCultivoRepository {

    private final JpaTipoCultivoRepository tipos;
    private final JpaVariedadCultivoRepository variedades;

    public CatalogoCultivoRepositoryImpl(JpaTipoCultivoRepository tipos,
                                         JpaVariedadCultivoRepository variedades) {
        this.tipos = tipos;
        this.variedades = variedades;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoCultivo> listarTipos(boolean soloActivos) {
        return tipos.findAllByOrderByNombreAsc().stream()
                .filter(t -> !soloActivos || t.isActivo())
                .map(t -> toDomain(t, soloActivos))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TipoCultivo> buscarTipo(Long id) {
        return id == null ? Optional.empty() : tipos.findById(id).map(t -> toDomain(t, false));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VariedadCultivo> buscarVariedad(Long id) {
        return id == null ? Optional.empty() : variedades.findById(id).map(CatalogoCultivoRepositoryImpl::toDomain);
    }

    @Override
    public boolean existeTipo(String nombre) {
        return tipos.existsByNombreIgnoreCase(nombre);
    }

    @Override
    public boolean existeVariedad(Long tipoCultivoId, String nombre) {
        return variedades.existsByTipoCultivoIdAndNombreIgnoreCase(tipoCultivoId, nombre);
    }

    @Override
    @Transactional
    public TipoCultivo guardarTipo(TipoCultivo tipo) {
        TipoCultivoEntity e = tipo.getId() != null
                ? tipos.findById(tipo.getId()).orElseGet(TipoCultivoEntity::new)
                : new TipoCultivoEntity();
        e.setNombre(tipo.getNombre());
        e.setActivo(tipo.isActivo());
        return toDomain(tipos.save(e), false);
    }

    @Override
    @Transactional
    public VariedadCultivo guardarVariedad(VariedadCultivo v) {
        VariedadCultivoEntity e = v.getId() != null
                ? variedades.findById(v.getId()).orElseGet(VariedadCultivoEntity::new)
                : new VariedadCultivoEntity();
        e.setNombre(v.getNombre());
        e.setActivo(v.isActivo());
        e.setTipoCultivo(tipos.getReferenceById(v.getTipoCultivoId()));
        VariedadCultivoEntity guardada = variedades.save(e);
        return variedades.findById(guardada.getId()).map(CatalogoCultivoRepositoryImpl::toDomain).orElseThrow();
    }

    @Override
    public long contarTipos() {
        return tipos.count();
    }

    private static TipoCultivo toDomain(TipoCultivoEntity t, boolean soloActivas) {
        List<VariedadCultivo> vs = t.getVariedades().stream()
                .filter(v -> !soloActivas || v.isActivo())
                .map(CatalogoCultivoRepositoryImpl::toDomain)
                .toList();
        return new TipoCultivo(t.getId(), t.getNombre(), t.isActivo(), vs);
    }

    private static VariedadCultivo toDomain(VariedadCultivoEntity v) {
        TipoCultivoEntity t = v.getTipoCultivo();
        return new VariedadCultivo(v.getId(), v.getNombre(), v.isActivo(),
                t.getId(), t.getNombre(), t.isActivo());
    }
}
