package com.agroruta.configuration.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Pruebas unitarias - FotoPerfil")
class FotoPerfilTest {

    @Test
    @DisplayName("Acepta vacío, null y enlaces cortos como antes")
    void aceptaValoresDeAntes() {
        assertNull(FotoPerfil.validar(null));
        assertEquals("", FotoPerfil.validar(""));
        assertEquals("https://cdn.agroruta.com/foto.jpg", FotoPerfil.validar("https://cdn.agroruta.com/foto.jpg"));
    }

    @Test
    @DisplayName("Acepta una foto subida en JPEG, PNG o WebP")
    void aceptaFotoEmbebida() {
        String jpg = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ==";
        assertEquals(jpg, FotoPerfil.validar(jpg));
        assertEquals("data:image/png;base64,iVBORw0KGgo=", FotoPerfil.validar("data:image/png;base64,iVBORw0KGgo="));
    }

    @Test
    @DisplayName("Rechaza archivos que no son imágenes o con contenido raro")
    void rechazaOtrosTipos() {
        assertThrows(IllegalArgumentException.class, () -> FotoPerfil.validar("data:text/html;base64,PGgxPg=="));
        assertThrows(IllegalArgumentException.class, () -> FotoPerfil.validar("data:image/svg+xml;base64,PHN2Zz4="));
        assertThrows(IllegalArgumentException.class, () -> FotoPerfil.validar("data:image/jpeg;base64,<script>"));
    }

    @Test
    @DisplayName("Rechaza fotos muy pesadas y enlaces muy largos")
    void rechazaTamanosExcesivos() {
        String pesada = "data:image/jpeg;base64," + "A".repeat(FotoPerfil.MAX_EMBEBIDA);
        assertThrows(IllegalArgumentException.class, () -> FotoPerfil.validar(pesada));
        String enlace = "https://x.co/" + "a".repeat(FotoPerfil.MAX_ENLACE);
        assertThrows(IllegalArgumentException.class, () -> FotoPerfil.validar(enlace));
    }
}
