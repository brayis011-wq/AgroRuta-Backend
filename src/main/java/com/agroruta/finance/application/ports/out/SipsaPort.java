package com.agroruta.finance.application.ports.out;

import com.agroruta.finance.domain.PrecioReferencia;

import java.io.InputStream;
import java.util.List;

/** Precios mayoristas semanales que publica SIPSA (DANE). */
public interface SipsaPort {

    /** El promedio nacional por producto de las últimas {@code semanas} semanas publicadas. */
    List<PrecioReferencia> preciosRecientes(int semanas);

    /**
     * Lo mismo, pero leído de una respuesta del DANE ya descargada (la sube un PC en Colombia, porque el
     * DANE no deja conectar desde servidores de afuera como Render).
     */
    default List<PrecioReferencia> desdeRespuesta(InputStream xml, int semanas) {
        throw new UnsupportedOperationException("Este SIPSA no lee respuestas descargadas");
    }
}
