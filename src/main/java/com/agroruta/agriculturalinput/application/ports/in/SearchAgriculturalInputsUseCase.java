package com.agroruta.agriculturalinput.application.ports.in;

import com.agroruta.agriculturalinput.domain.AgriculturalInput;
import java.util.List;

public interface SearchAgriculturalInputsUseCase {

    List<AgriculturalInput> search(String query);
}
