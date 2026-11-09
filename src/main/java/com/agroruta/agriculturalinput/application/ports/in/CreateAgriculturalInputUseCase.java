package com.agroruta.agriculturalinput.application.ports.in;

import com.agroruta.agriculturalinput.domain.AgriculturalInput;

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
            Integer reentradaHoras,
            Integer diasCarencia
    ) {
        /** Sin días de carencia (compatibilidad) */
        public CreateAgriculturalInputCommand(String nombre, String tipo, String unidadSugerida, Double dosisSugerida, Integer reentradaHoras) {
            this(nombre, tipo, unidadSugerida, dosisSugerida, reentradaHoras, null);
        }
    }
}