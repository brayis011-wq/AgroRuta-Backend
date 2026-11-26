package com.agroruta.finance.infrastructure.sipsa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agroruta.shared.exception.ExternalApiException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Los reintentos al consultar SIPSA, contra un servidor falso en la misma máquina. */
class SipsaSoapAdapterTest {

    private static final String RESPUESTA = """
            <S:Envelope xmlns:S="http://www.w3.org/2003/05/soap-envelope"><S:Body>
            <ns2:promediosSipsaSemanaMadrResponse xmlns:ns2="http://servicios.sipsa.co.gov.dane/">
            <return><artiNombre>Papa criolla</artiNombre><fechaIni>2026-11-14T00:00:00-05:00</fechaIni>
            <promedioKg>2500</promedioKg><minimoKg>2000</minimoKg><maximoKg>3000</maximoKg></return>
            </ns2:promediosSipsaSemanaMadrResponse></S:Body></S:Envelope>""";

    private HttpServer servidor;

    @AfterEach
    void apagar() {
        if (servidor != null) servidor.stop(0);
    }

    /** Responde con los códigos dados en orden (el último se repite); 200 lleva la respuesta de SIPSA */
    private String levantar(AtomicInteger llamadas, int... codigos) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/sipsa", ex -> {
            int i = llamadas.getAndIncrement();
            int codigo = codigos[Math.min(i, codigos.length - 1)];
            byte[] cuerpo = (codigo == 200 ? RESPUESTA : "error").getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(codigo, cuerpo.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(cuerpo);
            }
        });
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/sipsa";
    }

    @Test
    void siElServidorFallaSeReintentaYAlaTerceraTraeLosPrecios() throws IOException {
        AtomicInteger llamadas = new AtomicInteger();
        var sipsa = new SipsaSoapAdapter(levantar(llamadas, 503, 502, 200), Duration.ZERO);
        var precios = sipsa.preciosRecientes(4);
        assertThat(llamadas.get()).isEqualTo(3);
        assertThat(precios)
                .singleElement()
                .satisfies(p -> assertThat(p.producto()).isEqualTo("Papa criolla"));
    }

    @Test
    void despuesDeTresFallosAvisaConElMotivo() throws IOException {
        AtomicInteger llamadas = new AtomicInteger();
        var sipsa = new SipsaSoapAdapter(levantar(llamadas, 503), Duration.ZERO);
        assertThatThrownBy(() -> sipsa.preciosRecientes(4))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("503");
        assertThat(llamadas.get()).isEqualTo(SipsaSoapAdapter.INTENTOS);
    }

    @Test
    void unErrorQueNoEsDelServidorNoSeReintenta() throws IOException {
        AtomicInteger llamadas = new AtomicInteger();
        var sipsa = new SipsaSoapAdapter(levantar(llamadas, 404), Duration.ZERO);
        assertThatThrownBy(() -> sipsa.preciosRecientes(4)).hasMessageContaining("404");
        assertThat(llamadas.get()).isEqualTo(1);
    }

    @Test
    void siNoHayConexionTambienSeReintenta() {
        // Un puerto donde no escucha nadie: la conexión falla igual que un corte de la conexión segura
        var sipsa = new SipsaSoapAdapter("http://127.0.0.1:9/sipsa", Duration.ZERO);
        assertThatThrownBy(() -> sipsa.preciosRecientes(4))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("No se pudo consultar SIPSA");
    }
}
