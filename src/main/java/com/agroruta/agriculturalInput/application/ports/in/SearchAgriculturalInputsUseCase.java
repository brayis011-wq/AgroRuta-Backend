package com.agroruta.agriculturalInput.application.ports.in;

import com.agroruta.agriculturalInput.domain.AgriculturalInput;
import java.util.List;

public interface SearchAgriculturalInputsUseCase {

    List<AgriculturalInput> search(String query);
}
