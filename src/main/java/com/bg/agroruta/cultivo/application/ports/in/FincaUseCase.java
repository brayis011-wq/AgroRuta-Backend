package com.bg.agroruta.cultivo.application.ports.in;

import com.bg.agroruta.cultivo.domain.Finca;
import java.util.List;

public interface FincaUseCase {
    Finca registrarFinca(Finca finca);
    Finca buscarFincaPorId(Long id);
    List<Finca> listarFincasPorAgricultor(Long agricultorId);
}