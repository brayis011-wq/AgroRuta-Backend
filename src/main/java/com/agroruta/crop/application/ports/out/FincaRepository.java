package com.agroruta.crop.application.ports.out;

import com.agroruta.crop.domain.Finca;

import java.util.List;
import java.util.Optional;

/**
 * No hay borrado físico: eliminar una finca es {@link Finca#eliminar} + {@link #save}.
 * findById también devuelve fincas eliminadas (los reportes las necesitan);
 * las demás consultas solo ven las vigentes.
 */
public interface FincaRepository {
    Finca save(Finca finca);
    Optional<Finca> findById(Long id);
    /** Fincas vigentes del agricultor */
    List<Finca> findByAgricultorId(Long agricultorId);
    /** ¿Hay una finca vigente del agricultor con ese nombre? */
    boolean existsByNombreAndAgricultorId(String nombre, Long agricultorId);
}
