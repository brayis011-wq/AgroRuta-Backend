package com.agroruta.finance.infrastructure.sipsa;

import com.agroruta.finance.application.ports.out.SipsaPort;
import com.agroruta.finance.domain.AgregadorSipsa;
import com.agroruta.finance.domain.PrecioReferencia;
import com.agroruta.shared.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
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

    private final String url;
    private final HttpClient http;

    public SipsaSoapAdapter(@Value("${sipsa.url:https://appweb.dane.gov.co/sipsaWS/SrvSipsaUpraBeanService}") String url) {
        this.url = url;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public List<PrecioReferencia> preciosRecientes(int semanas) {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(3))
                .header("Content-Type", "application/soap+xml; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(SOBRE))
                .build();
        try {
            HttpResponse<InputStream> r = http.send(pedido, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream cuerpo = r.body()) {
                if (r.statusCode() != 200) {
                    throw new ExternalApiException("SIPSA respondió " + r.statusCode());
                }
                return leer(cuerpo).ultimasSemanas(semanas);
            }
        } catch (IOException e) {
            throw new ExternalApiException("No se pudo consultar SIPSA: " + e.getMessage());
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
