package com.agroruta.agriculturalinput.application.ports.in;

import com.agroruta.agriculturalinput.domain.AgriculturalInput;

public interface GetAgriculturalInputByIdUseCase {
    AgriculturalInput getById(Long id);
}

