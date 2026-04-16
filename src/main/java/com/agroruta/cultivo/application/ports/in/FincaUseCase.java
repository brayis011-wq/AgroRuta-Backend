package com.agroruta.cultivo.application.ports.in;

import com.agroruta.cultivo.domain.Finca;
import java.util.List;

public interface FincaUseCase {
    Finca registrarFinca(String nombre, String ubicacion, Double hectareas, Long agricultorId);
    Finca buscarFincaPorId(Long id);
    List<Finca> listarFincasPorAgricultor(Long agricultorId);
}