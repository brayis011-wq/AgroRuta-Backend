package com.agroruta.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlantillaCorreo")
class PlantillaCorreoTest {

    @Test
    @DisplayName("escapa el HTML de los datos (un nombre no puede meter etiquetas)")
    void escapa() {
        Correo c = PlantillaCorreo.con("Hola <b>Ana</b>")
                .parrafo("Lote \"5\" & <script>alert(1)</script>")
                .lista(List.<String[]>of(new String[]{"<i>Finca</i>", "ok"}))
                .para("ana@x.com", "Asunto");
        assertThat(c.html()).doesNotContain("<script>").doesNotContain("<b>Ana</b>").doesNotContain("<i>Finca</i>")
                .contains("&lt;script&gt;").contains("&quot;5&quot; &amp;");
        assertThat(c.para()).isEqualTo("ana@x.com");
        assertThat(c.asunto()).isEqualTo("Asunto");
    }

    @Test
    @DisplayName("la versión de texto lleva lo mismo, con el enlace del botón")
    void textoPlano() {
        Correo c = PlantillaCorreo.con("Título")
                .parrafo("Párrafo")
                .seccion("Sección")
                .lista(List.<String[]>of(new String[]{"Lote 1", "· hoy toca fumigar."}))
                .aviso("Cuidado")
                .boton("Abrir", "https://agroruta.app/x?token=abc")
                .pie("Pie")
                .para("a@b.co", "A");
        assertThat(c.texto()).contains("Título", "Párrafo", "SECCIÓN", " - Lote 1 · hoy toca fumigar.", "! Cuidado",
                "Abrir: https://agroruta.app/x?token=abc", "Pie");
        assertThat(c.html()).contains("href=\"https://agroruta.app/x?token=abc\"");
    }
}
