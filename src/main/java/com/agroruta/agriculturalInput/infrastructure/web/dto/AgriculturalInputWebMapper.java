package com.agroruta.agriculturalInput.infrastructure.web.dto;

import com.agroruta.agriculturalInput.application.ports.in.CreateAgriculturalInputUseCase.CreateAgriculturalInputCommand;
import com.agroruta.agriculturalInput.application.ports.in.UpdateAgriculturalInputUseCase.UpdateAgriculturalInputCommand;
import com.agroruta.agriculturalInput.domain.AgriculturalInput;

/**
 * Mapper de la capa web.
 * Separa la responsabilidad de conversión DTO ↔ Dominio del controlador.
 */
public class AgriculturalInputWebMapper {

    private AgriculturalInputWebMapper() {}

    public static AgriculturalInputResponse toResponse(AgriculturalInput domain) {
        return AgriculturalInputResponse.builder()
                .id(domain.getId())
                .nombre(domain.getNombre())
                .tipo(domain.getTipo().name())
                .tipoDisplay(domain.getTipo().getDisplayName())
                .unidadSugerida(domain.getUnidadSugerida().name())
                .dosisSugerida(domain.getDosisSugerida())
                .reentradaHoras(domain.getReentradaHoras())
                .activo(domain.isActivo())
                .creadoEn(domain.getCreadoEn())
                .build();
    }

    public static CreateAgriculturalInputCommand toCreateCommand(AgriculturalInputRequest request) {
        return new CreateAgriculturalInputCommand(
                request.getNombre(),
                request.getTipo(),
                request.getUnidadSugerida(),
                request.getDosisSugerida(),
                request.getReentradaHoras()
        );
    }

    public static UpdateAgriculturalInputCommand toUpdateCommand(AgriculturalInputRequest request) {
        return new UpdateAgriculturalInputCommand(
                request.getNombre(),
                request.getTipo(),
                request.getUnidadSugerida(),
                request.getDosisSugerida(),
                request.getReentradaHoras()
        );
    }
}