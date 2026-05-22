package com.agroruta.agriculturalInput.application.ports.in;

import com.agroruta.agriculturalInput.domain.AgriculturalInput;

public interface GetAgriculturalInputByIdUseCase {
    AgriculturalInput getById(Long id);
}

