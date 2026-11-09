package com.agroruta.agriculturalinput.application.ports.in;

import com.agroruta.agriculturalinput.domain.AgriculturalInput;

import java.util.List;

/** Vista y control del catálogo para el Administrador: ver inactivos y volver a activarlos. */
public interface ManageAgriculturalInputStatusUseCase {

    /** Todos los insumos, activos e inactivos, ordenados por nombre. */
    List<AgriculturalInput> getAllIncludingInactive();

    /** Activa o desactiva un insumo. No se borra nada para no romper fumigaciones ya registradas. */
    AgriculturalInput changeStatus(Long id, boolean activo);
}
