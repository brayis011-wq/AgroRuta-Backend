package com.agroruta.finance.domain;

import com.agroruta.finance.infrastructure.sipsa.SipsaSoapAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Precios de referencia de SIPSA")
class PreciosReferenciaTest {

    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 14);

    private static PrecioReferencia p(String producto) {
        return new PrecioReferencia(producto, SEMANA, new BigDecimal("1000"), null, null, 3);
    }

    private static final List<PrecioReferencia> SEMANA_SIPSA = List.of(
            p("Aguacate Choquette"), p("Aguacate común"), p("Aguacate Hass"), p("Aguacate papelillo"),
            p("Mora de Castilla"), p("Papa criolla limpia"), p("Papa criolla sucia"), p("Papa parda pastusa"),
            p("Papa suprema"), p("Tomate chonto"), p("Tomate chonto regional"), p("Tomate de árbol"),
            p("Uchuva con cáscara"), p("Lulo"), p("Café molido"), p("Papaya Maradol"));

    @Test
    void encuentraLaVariedad() {
        assertThat(BuscadorPrecio.mejor(SEMANA_SIPSA, "Aguacate", "Hass")).get()
                .extracting(PrecioReferencia::producto).isEqualTo("Aguacate Hass");
        assertThat(BuscadorPrecio.mejor(SEMANA_SIPSA, "Mora", "Castilla")).get()
                .extracting(PrecioReferencia::producto).isEqualTo("Mora de Castilla");
        assertThat(BuscadorPrecio.mejor(SEMANA_SIPSA, "Papa", "Criolla")).get()
                .extracting(PrecioReferencia::producto).asString().startsWith("Papa criolla");
        assertThat(BuscadorPrecio.mejor(SEMANA_SIPSA, "Tomate", "Chonto")).get()
                .extracting(PrecioReferencia::producto).isEqualTo("Tomate chonto");
    }

    @Test
    void sinVariedadParecidaPrefiereElComun() {
        assertThat(BuscadorPrecio.mejor(SEMANA_SIPSA, "Aguacate", "Lorena")).get()
                .extracting(PrecioReferencia::producto).isEqualTo("Aguacate común");
    }

    @Test
    void noConfundeProductosQueEmpiezanIgual() {
        assertThat(BuscadorPrecio.candidatos(SEMANA_SIPSA, "Papa", null))
                .extracting(PrecioReferencia::producto).doesNotContain("Papaya Maradol");
        assertThat(BuscadorPrecio.mejor(SEMANA_SIPSA, "Uchuva", "Colombia")).get()
                .extracting(PrecioReferencia::producto).isEqualTo("Uchuva con cáscara");
    }

    @Test
    void losProductosProcesadosNoSeSugieren() {
        assertThat(BuscadorPrecio.candidatos(SEMANA_SIPSA, "Café", "Castillo")).isEmpty();
    }

    @Test
    void elCacaoNoEstaEnSipsa() {
        // Para el cacao y el café pergamino la persona pone su propio precio
        assertThat(BuscadorPrecio.candidatos(SEMANA_SIPSA, "Cacao", "CCN-51")).isEmpty();
    }

    @Test
    void ignoraTildesYMayusculas() {
        assertThat(BuscadorPrecio.normalizar("  Plátano  Hartón ")).isEqualTo("platano harton");
    }

    @Test
    void elAgregadorPromediaLosMercadosDeCadaSemana() {
        AgregadorSipsa a = new AgregadorSipsa();
        a.agregar("Lulo", SEMANA, new BigDecimal("3000"), new BigDecimal("2800"), new BigDecimal("3200"));
        a.agregar("Lulo", SEMANA, new BigDecimal("4000"), new BigDecimal("3900"), new BigDecimal("4100"));
        a.agregar("Lulo", SEMANA.minusWeeks(1), new BigDecimal("5000"), null, null);
        a.agregar("Lulo", SEMANA.minusWeeks(2), new BigDecimal("6000"), null, null);
        a.agregar(null, SEMANA, new BigDecimal("1"), null, null);
        List<PrecioReferencia> dos = a.ultimasSemanas(2);
        assertThat(dos).hasSize(2);
        PrecioReferencia ultima = dos.stream().filter(x -> x.semana().equals(SEMANA)).findFirst().orElseThrow();
        assertThat(ultima.precioKg()).isEqualByComparingTo("3500");
        assertThat(ultima.minimoKg()).isEqualByComparingTo("2800");
        assertThat(ultima.maximoKg()).isEqualByComparingTo("4100");
        assertThat(ultima.mercados()).isEqualTo(2);
    }

    @Test
    void leeLaRespuestaSoapDeSipsa() {
        String xml = """
                <?xml version='1.0' encoding='UTF-8'?><S:Envelope xmlns:S="http://www.w3.org/2003/05/soap-envelope"><S:Body>
                <ns2:promediosSipsaSemanaMadrResponse xmlns:ns2="http://servicios.sipsa.co.gov.dane/">
                <return><artiId>1</artiId><artiNombre>Lulo</artiNombre><fechaIni>2026-11-14T00:00:00-05:00</fechaIni>
                <fuenNombre>Bogotá, Corabastos</fuenNombre><maximoKg>3200</maximoKg><minimoKg>2800</minimoKg><promedioKg>3000</promedioKg></return>
                <return><artiId>1</artiId><artiNombre>Lulo</artiNombre><fechaIni>2026-11-14T00:00:00-05:00</fechaIni>
                <fuenNombre>Medellín, Central Mayorista</fuenNombre><maximoKg>4100</maximoKg><minimoKg>3900</minimoKg><promedioKg>4000</promedioKg></return>
                </ns2:promediosSipsaSemanaMadrResponse></S:Body></S:Envelope>""";
        List<PrecioReferencia> precios = SipsaSoapAdapter.leer(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                .ultimasSemanas(4);
        assertThat(precios).singleElement().satisfies(x -> {
            assertThat(x.producto()).isEqualTo("Lulo");
            assertThat(x.semana()).isEqualTo(SEMANA);
            assertThat(x.precioKg()).isEqualByComparingTo("3500");
        });
    }
}
