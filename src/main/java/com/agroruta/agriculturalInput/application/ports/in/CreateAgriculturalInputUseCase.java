package com.agroruta.agriculturalInput.application.ports.in;

import com.agroruta.agriculturalInput.domain.AgriculturalInput;

public interface CreateAgriculturalInputUseCase {

    AgriculturalInput create(CreateAgriculturalInputCommand command);

    /**
     * Command object: transporta los datos validados desde la web hacia la aplicación.
     * Se usa record para inmutabilidad garantizada.
     */
    record CreateAgriculturalInputCommand(
            String nombre,
            String tipo,
            String unidadSugerida,
            Double dosisSugerida,
            Integer reentradaHoras
    ) {}
}