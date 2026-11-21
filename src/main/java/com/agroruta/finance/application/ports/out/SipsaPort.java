package com.agroruta.finance.application.ports.out;

import com.agroruta.finance.domain.PrecioReferencia;

import java.util.List;

/** Precios mayoristas semanales que publica SIPSA (DANE). */
public interface SipsaPort {

    /** El promedio nacional por producto de las últimas {@code semanas} semanas publicadas. */
    List<PrecioReferencia> preciosRecientes(int semanas);
}
