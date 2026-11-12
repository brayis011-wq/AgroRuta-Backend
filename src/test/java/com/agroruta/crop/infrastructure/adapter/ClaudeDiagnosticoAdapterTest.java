package com.agroruta.crop.infrastructure.adapter;

import com.agroruta.crop.domain.ContextoCultivo;
import com.agroruta.shared.exception.ExternalApiException;
import com.anthropic.models.beta.messages.MessageCreateParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** No llama al servicio: revisa cómo queda armada la petición. */
class ClaudeDiagnosticoAdapterTest {

    private static final ContextoCultivo CAFE = new ContextoCultivo("Café", "Castillo", "crecimiento", 40L, "polvo naranja");
    private static final byte[] FOTO = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};

    @Test
    void sinClaveQuedaApagado() {
        ClaudeDiagnosticoAdapter adaptador = new ClaudeDiagnosticoAdapter(" ", "claude-opus-5-5", true);
        assertFalse(adaptador.disponible());
        assertThrows(ExternalApiException.class, () -> adaptador.analizar(FOTO, CAFE));
    }

    @Test
    void armaLaPeticionConLaFotoElContextoYLaFormaDeLaRespuesta() {
        ClaudeDiagnosticoAdapter adaptador = new ClaudeDiagnosticoAdapter("clave-de-prueba", "claude-opus-5-5", true);
        assertTrue(adaptador.disponible());

        MessageCreateParams peticion = adaptador.parametros(FOTO, CAFE).rawParams();

        assertEquals("claude-opus-5-5", peticion.model().asString());
        assertEquals(16000L, peticion.maxTokens());
        assertTrue(peticion.system().isPresent());
        assertEquals(1, peticion.messages().size());
        // Respaldo del servidor cuando el modelo declina una foto
        assertTrue(peticion.fallbacks().isPresent());
        assertTrue(peticion.betas().isPresent());
        // La respuesta se pide con una forma fija (esPlanta, problema, tipo...)
        String forma = peticion.outputConfig().orElseThrow().toString();
        for (String campo : new String[] {"esPlanta", "problema", "tipo", "confianza", "sintomas", "recomendaciones",
                "otrasPosibilidades", "advertencia", "DANO_AMBIENTAL", "BAJA"}) {
            assertTrue(forma.contains(campo), "falta " + campo + " en " + forma);
        }
    }

    @Test
    void elRespaldoSePuedeApagar() {
        MessageCreateParams peticion = new ClaudeDiagnosticoAdapter("clave-de-prueba", "claude-opus-5-5", false)
                .parametros(FOTO, CAFE).rawParams();
        assertFalse(peticion.fallbacks().isPresent());
    }

    @Test
    void describeLaSiembraAunqueFaltenDatos() {
        String completo = ClaudeDiagnosticoAdapter.describir(CAFE);
        assertTrue(completo.contains("Cultivo: Café"));
        assertTrue(completo.contains("Días desde la siembra: 40"));
        assertTrue(completo.contains("«polvo naranja»"));

        String vacio = ClaudeDiagnosticoAdapter.describir(new ContextoCultivo(null, " ", null, null, null));
        assertTrue(vacio.contains("Cultivo: sin dato"));
        assertTrue(vacio.contains("(no escribió nota)"));
    }
}
