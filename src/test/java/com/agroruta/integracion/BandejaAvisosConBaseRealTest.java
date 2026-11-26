package com.agroruta.integracion;

import static org.assertj.core.api.Assertions.assertThat;

import com.agroruta.notification.application.ports.out.BandejaAvisosRepository;
import com.agroruta.notification.domain.AvisoBandeja;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** La bandeja de avisos contra MySQL real: la migración V18 y el INSERT ... ON DUPLICATE KEY UPDATE. */
@DisplayName("Bandeja de avisos con base real")
class BandejaAvisosConBaseRealTest extends ConBaseReal {

    @Autowired
    private BandejaAvisosRepository bandeja;

    private long usuario(String nombre) {
        return insertar(
                "INSERT INTO usuarios (activo, email, fecha_creacion, nombre, password, rol) VALUES (1, ?, ?, ?, 'sin-clave', 'Trabajador')",
                nombre + "-" + System.nanoTime() + "@agroruta.test",
                LocalDateTime.now(),
                nombre);
    }

    private AvisoBandeja.Nuevo pago(long usuario, String etiqueta, String monto) {
        return new AvisoBandeja.Nuevo(
                usuario,
                AvisoBandeja.Tipo.PAGO_REGISTRADO,
                "Te registraron un pago",
                "Recibiste $ " + monto,
                Map.of("monto", monto),
                "/portal/payments",
                etiqueta);
    }

    @Test
    @DisplayName("guarda, lista del más nuevo al más viejo y cuenta los no leídos")
    void guardaYLista() {
        long ana = usuario("Ana");
        bandeja.guardar(pago(ana, "pago-1", "100"), Instant.parse("2026-11-01T10:00:00Z"));
        bandeja.guardar(pago(ana, "pago-2", "200"), Instant.parse("2026-11-02T10:00:00Z"));

        List<AvisoBandeja> avisos = bandeja.recientes(ana, 30);
        assertThat(avisos).extracting(AvisoBandeja::cuerpo).containsExactly("Recibiste $ 200", "Recibiste $ 100");
        assertThat(avisos.get(0).tipo()).isEqualTo(AvisoBandeja.Tipo.PAGO_REGISTRADO);
        assertThat(avisos.get(0).datos()).containsEntry("monto", "200");
        assertThat(avisos.get(0).creadoEn()).isEqualTo(Instant.parse("2026-11-02T10:00:00Z"));
        assertThat(bandeja.noLeidos(ana)).isEqualTo(2);
    }

    @Test
    @DisplayName("la misma etiqueta no repite el aviso: lo actualiza y vuelve a quedar sin leer")
    void mismaEtiqueta() {
        long ana = usuario("Ana");
        bandeja.guardar(pago(ana, "resumen-2026-11-18", "100"), Instant.parse("2026-11-18T06:00:00Z"));
        long id = bandeja.recientes(ana, 30).get(0).id();
        bandeja.marcarLeido(ana, id, Instant.parse("2026-11-18T07:00:00Z"));
        assertThat(bandeja.noLeidos(ana)).isZero();

        bandeja.guardar(pago(ana, "resumen-2026-11-18", "300"), Instant.parse("2026-11-18T08:00:00Z"));
        List<AvisoBandeja> avisos = bandeja.recientes(ana, 30);
        assertThat(avisos).hasSize(1);
        assertThat(avisos.get(0).cuerpo()).isEqualTo("Recibiste $ 300");
        assertThat(avisos.get(0).leido()).isFalse();
    }

    @Test
    @DisplayName("nadie puede marcar ni ver los avisos de otra persona")
    void soloLosPropios() {
        long ana = usuario("Ana");
        long beto = usuario("Beto");
        bandeja.guardar(pago(ana, "pago-1", "100"), Instant.parse("2026-11-01T10:00:00Z"));
        long idDeAna = bandeja.recientes(ana, 30).get(0).id();

        assertThat(bandeja.recientes(beto, 30)).isEmpty();
        assertThat(bandeja.marcarLeido(beto, idDeAna, Instant.now())).isFalse();
        assertThat(bandeja.marcarTodosLeidos(beto, Instant.now())).isZero();
        assertThat(bandeja.noLeidos(ana)).isEqualTo(1);

        assertThat(bandeja.marcarTodosLeidos(ana, Instant.now())).isEqualTo(1);
        assertThat(bandeja.noLeidos(ana)).isZero();
    }

    @Test
    @DisplayName("al borrar la cuenta se borran sus avisos")
    void cascada() {
        long ana = usuario("Ana");
        bandeja.guardar(pago(ana, "pago-1", "100"), Instant.now());
        jdbc.update("DELETE FROM usuarios WHERE id = ?", ana);
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM avisos_bandeja WHERE usuario_id = ?", Integer.class, ana);
        assertThat(n).isZero();
    }
}
