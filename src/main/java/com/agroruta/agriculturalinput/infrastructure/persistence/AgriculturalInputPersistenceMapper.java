package com.agroruta.agriculturalinput.infrastructure.persistence;

import com.agroruta.agriculturalinput.domain.AgriculturalInput;
import com.agroruta.agriculturalinput.infrastructure.persistence.AgriculturalInputEntity;

/**
 * Mapper de persistencia.
 * Convierte entre el modelo de dominio y la entidad JPA.
 * Mantiene ambas capas completamente desacopladas.
 */
public class AgriculturalInputPersistenceMapper {

    private AgriculturalInputPersistenceMapper() {}

    public static AgriculturalInput toDomain(AgriculturalInputEntity entity) {
        AgriculturalInput domain = AgriculturalInput.create(
                entity.getNombre(),
                entity.getTipo(),
                entity.getUnidadSugerida(),
                entity.getDosisSugerida(),
                entity.getReentradaHoras(),
                entity.getDiasCarencia()
        );
        domain.setId(entity.getId());
        domain.setActivo(entity.isActivo());
        domain.setCreadoEn(entity.getCreadoEn());
        return domain;
    }

    public static AgriculturalInputEntity toEntity(AgriculturalInput domain) {
        AgriculturalInputEntity entity = new AgriculturalInputEntity();
        entity.setId(domain.getId());
        entity.setNombre(domain.getNombre());
        entity.setTipo(domain.getTipo());
        entity.setUnidadSugerida(domain.getUnidadSugerida());
        entity.setDosisSugerida(domain.getDosisSugerida());
        entity.setReentradaHoras(domain.getReentradaHoras());
        entity.setDiasCarencia(domain.getDiasCarencia());
        entity.setActivo(domain.isActivo());
        entity.setCreadoEn(domain.getCreadoEn());
        return entity;
    }
}