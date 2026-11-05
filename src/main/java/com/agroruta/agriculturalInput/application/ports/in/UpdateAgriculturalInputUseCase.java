package com.agroruta.agriculturalInput.application.ports.in;

import com.agroruta.agriculturalInput.domain.AgriculturalInput;

public interface UpdateAgriculturalInputUseCase {

    AgriculturalInput update(Long id, UpdateAgriculturalInputCommand command);

    record UpdateAgriculturalInputCommand(
            String nombre,
            String tipo,
            String unidadSugerida,
            Double dosisSugerida,
            Integer reentradaHoras,
            Integer diasCarencia
    ) {
        /** Sin días de carencia (compatibilidad) */
        public UpdateAgriculturalInputCommand(String nombre, String tipo, String unidadSugerida, Double dosisSugerida, Integer reentradaHoras) {
            this(nombre, tipo, unidadSugerida, dosisSugerida, reentradaHoras, null);
        }
    }
}
