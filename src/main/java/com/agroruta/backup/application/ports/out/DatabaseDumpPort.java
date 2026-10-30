package com.agroruta.backup.application.ports.out;

import java.nio.file.Path;

public interface DatabaseDumpPort {

    /** Vuelca toda la base de datos al archivo indicado. */
    void dump(Path target);

    /** Restaura la base de datos desde el archivo .sql indicado. */
    void restore(Path source);
}