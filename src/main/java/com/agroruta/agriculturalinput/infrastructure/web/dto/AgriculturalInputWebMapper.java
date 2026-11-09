package com.agroruta.agriculturalinput.infrastructure.web.dto;

import com.agroruta.agriculturalinput.application.ports.in.CreateAgriculturalInputUseCase.CreateAgriculturalInputCommand;
import com.agroruta.agriculturalinput.application.ports.in.UpdateAgriculturalInputUseCase.UpdateAgriculturalInputCommand;
import com.agroruta.agriculturalinput.domain.AgriculturalInput;

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
                .diasCarencia(domain.getDiasCarencia())
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
                request.getReentradaHoras(),
                request.getDiasCarencia()
        );
    }

    public static UpdateAgriculturalInputCommand toUpdateCommand(AgriculturalInputRequest request) {
        return new UpdateAgriculturalInputCommand(
                request.getNombre(),
                request.getTipo(),
                request.getUnidadSugerida(),
                request.getDosisSugerida(),
                request.getReentradaHoras(),
                request.getDiasCarencia()
        );
    }
}