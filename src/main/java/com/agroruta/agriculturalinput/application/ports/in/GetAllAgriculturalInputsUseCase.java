package com.agroruta.agriculturalinput.application.ports.in;

import com.agroruta.agriculturalinput.domain.AgriculturalInput;
import java.util.List;

public interface GetAllAgriculturalInputsUseCase {
    List<AgriculturalInput> getAll();
}
