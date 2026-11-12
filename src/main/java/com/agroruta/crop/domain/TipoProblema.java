package com.agroruta.crop.domain;

/** Qué clase de problema muestra la foto de un cultivo. */
public enum TipoProblema {
    /** Insectos, ácaros, babosas y otros animales que dañan la planta */
    PLAGA,
    /** Hongos, bacterias o virus */
    ENFERMEDAD,
    /** Falta o exceso de nutrientes */
    DEFICIENCIA,
    /** Sol, helada, agua, golpes o agroquímicos */
    DANO_AMBIENTAL,
    SANA,
    INDETERMINADO
}
