package com.agroruta.finance.infrastructure.sipsa;

import com.agroruta.finance.application.ports.out.SipsaPort;
import com.agroruta.finance.domain.AgregadorSipsa;
import com.agroruta.finance.domain.PrecioReferencia;
import com.agroruta.shared.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.SSLParameters;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Servicio web SOAP de SIPSA (DANE), método promediosSipsaSemanaMadr: precios mayoristas semanales
 * por producto y mercado del último año. La respuesta pesa unos 70 MB, así que se lee por partes
 * (StAX) y solo se guardan las sumas por producto y semana.
 */
@Component
public class SipsaSoapAdapter implements SipsaPort {

    static final String SOBRE = "<soap12:Envelope xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\""
            + " xmlns:s=\"http://servicios.sipsa.co.gov.dane/\"><soap12:Body><s:promediosSipsaSemanaMadr/>"
            + "</soap12:Body></soap12:Envelope>";

    /** Intentos por consulta: el servidor del DANE a veces corta la conexión segura desde fuera de Colombia */
    static final int INTENTOS = 3;

    private static final Logger log = LoggerFactory.getLogger(SipsaSoapAdapter.class);

    private final String url;
    /** Primer intento: HTTP/1.1 (el DANE no habla HTTP/2) con la negociación TLS normal */
    private final HttpClient http;
    /** Reintentos: solo TLS 1.2, lo más compatible con servidores y cortafuegos viejos */
    private final HttpClient compatible;
    private final Duration espera;

    @org.springframework.beans.factory.annotation.Autowired
    public SipsaSoapAdapter(@Value("${sipsa.url:https://appweb.dane.gov.co/sipsaWS/SrvSipsaUpraBeanService}") String url) {
        this(url, Duration.ofSeconds(5));
    }

    SipsaSoapAdapter(String url, Duration espera) {
        this.url = url;
        this.espera = espera;
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        SSLParameters tls12 = new SSLParameters();
        tls12.setProtocols(new String[] {"TLSv1.2"});
        this.compatible = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .sslParameters(tls12)
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Trae los precios. Si la conexión se cae (p. ej. "Remote host terminated the handshake") o el DANE
     * responde con un error del servidor, se reintenta hasta {@link #INTENTOS} veces con unos segundos de espera,
     * desde el segundo intento solo con TLS 1.2.
     */
    @Override
    public List<PrecioReferencia> preciosRecientes(int semanas) {
        ExternalApiException ultimo = null;
        for (int intento = 1; intento <= INTENTOS; intento++) {
            try {
                return consultar(intento == 1 ? http : compatible, semanas);
            } catch (Reintentable e) {
                ultimo = new ExternalApiException(e.getMessage());
                log.warn("SIPSA, intento {} de {}: {}", intento, INTENTOS, e.getMessage());
                if (intento < INTENTOS) esperar();
            }
        }
        throw ultimo;
    }

    /** Falla de la red o del servidor del DANE: vale la pena volver a intentar */
    static final class Reintentable extends RuntimeException {
        Reintentable(String mensaje) { super(mensaje); }
    }

    private void esperar() {
        try {
            Thread.sleep(espera.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException("Se interrumpió la consulta a SIPSA");
        }
    }

    /** Una consulta con ese cliente */
    List<PrecioReferencia> consultar(HttpClient cliente, int semanas) {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(3))
                .header("Content-Type", "application/soap+xml; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(SOBRE))
                .build();
        try {
            HttpResponse<InputStream> r = cliente.send(pedido, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream cuerpo = r.body()) {
                if (r.statusCode() >= 500) throw new Reintentable("SIPSA respondió " + r.statusCode());
                if (r.statusCode() != 200) {
                    throw new ExternalApiException("SIPSA respondió " + r.statusCode());
                }
                return leer(cuerpo).ultimasSemanas(semanas);
            }
        } catch (IOException e) {
            throw new Reintentable("No se pudo consultar SIPSA: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException("Se interrumpió la consulta a SIPSA");
        }
    }

    /** Lee las filas {@code <return>} de la respuesta y las suma por producto y semana */
    public static AgregadorSipsa leer(InputStream xml) {
        AgregadorSipsa agregador = new AgregadorSipsa();
        XMLInputFactory f = XMLInputFactory.newFactory();
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try {
            XMLStreamReader x = f.createXMLStreamReader(xml, "UTF-8");
            String producto = null, campo = null;
            LocalDate semana = null;
            BigDecimal promedio = null, minimo = null, maximo = null;
            StringBuilder texto = new StringBuilder();
            while (x.hasNext()) {
                int evento = x.next();
                if (evento == XMLStreamConstants.START_ELEMENT) {
                    String nombre = x.getLocalName();
                    if ("return".equals(nombre)) {
                        producto = null; semana = null; promedio = null; minimo = null; maximo = null;
                    }
                    campo = nombre;
                    texto.setLength(0);
                } else if (evento == XMLStreamConstants.CHARACTERS && campo != null) {
                    texto.append(x.getText());
                } else if (evento == XMLStreamConstants.END_ELEMENT) {
                    String nombre = x.getLocalName();
                    String valor = texto.toString().trim();
                    switch (nombre) {
                        case "artiNombre" -> producto = valor;
                        case "fechaIni" -> semana = fecha(valor);
                        case "promedioKg" -> promedio = numero(valor);
                        case "minimoKg" -> minimo = numero(valor);
                        case "maximoKg" -> maximo = numero(valor);
                        case "return" -> agregador.agregar(producto, semana, promedio, minimo, maximo);
                        default -> { }
                    }
                    campo = null;
                    texto.setLength(0);
                }
            }
            x.close();
        } catch (XMLStreamException e) {
            throw new ExternalApiException("La respuesta de SIPSA no se pudo leer: " + e.getMessage());
        }
        return agregador;
    }

    private static LocalDate fecha(String v) {
        try {
            return v.isEmpty() ? null : OffsetDateTime.parse(v).toLocalDate();
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(v.substring(0, Math.min(10, v.length())));
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    private static BigDecimal numero(String v) {
        try {
            return v.isEmpty() ? null : new BigDecimal(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
