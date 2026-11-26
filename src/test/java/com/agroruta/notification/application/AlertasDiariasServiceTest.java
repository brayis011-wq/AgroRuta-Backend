package com.agroruta.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agroruta.crop.application.ports.in.AlertasCultivoUseCase;
import com.agroruta.crop.application.ports.out.FincaRepository;
import com.agroruta.crop.domain.AlertaCultivo;
import com.agroruta.crop.domain.Finca;
import com.agroruta.inventory.application.ports.in.StockBajoUseCase;
import com.agroruta.notification.application.ports.in.PreferenciasCorreoUseCase;
import com.agroruta.notification.application.ports.out.CorreoPort;
import com.agroruta.notification.domain.Correo;
import com.agroruta.notification.domain.PreferenciasCorreo;
import com.agroruta.user.application.ports.out.UsuarioRepository;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.weather.application.ports.in.WeatherUseCase;
import com.agroruta.weather.domain.Pronostico;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("AlertasDiariasService")
class AlertasDiariasServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PreferenciasCorreoUseCase preferencias = mock(PreferenciasCorreoUseCase.class);
    private final AlertasCultivoUseCase alertas = mock(AlertasCultivoUseCase.class);
    private final FincaRepository fincas = mock(FincaRepository.class);
    private final WeatherUseCase clima = mock(WeatherUseCase.class);
    private final CorreoPort correo = mock(CorreoPort.class);
    private final StockBajoUseCase stockBajo = mock(StockBajoUseCase.class);
    private final LocalDate hoy = LocalDate.of(2026, 11, 5);
    private AlertasDiariasService service;

    private final Usuario ana = new Usuario(1L, "Ana", "ana@x.com", "h", Rol.Agricultor);
    private final Usuario luis = new Usuario(2L, "Luis", "luis@x.com", "h", Rol.Agricultor);
    private final Usuario admin = new Usuario(3L, "Admin", "admin@x.com", "h", Rol.Administrador);

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-05T11:00:00Z"), ZoneId.of("America/Bogota"));
        service = new AlertasDiariasService(
                usuarios,
                preferencias,
                alertas,
                fincas,
                clima,
                new MensajesCorreo("https://agroruta.app"),
                correo,
                stockBajo,
                reloj);
        when(usuarios.findAllActivos()).thenReturn(List.of(ana, luis, admin));
        when(preferencias.obtener(anyLong())).thenAnswer(i -> PreferenciasCorreo.porDefecto(i.getArgument(0)));
        when(correo.enviar(any())).thenReturn(true);
    }

    private AlertaCultivo fumigarHoy() {
        return new AlertaCultivo(
                AlertaCultivo.Tipo.FUMIGACION_HOY, "La Esperanza", "Lote 1", "Uchuva · Colombia", null, null, hoy);
    }

    @Test
    @DisplayName("solo manda a agricultores con algo para hoy")
    void soloConAlertas() {
        when(alertas.alertasDelDia(1L, hoy)).thenReturn(List.of(fumigarHoy()));
        when(alertas.alertasDelDia(2L, hoy)).thenReturn(List.of());

        assertThat(service.enviarResumenDelDia(false)).isEqualTo(1);

        ArgumentCaptor<Correo> c = ArgumentCaptor.forClass(Correo.class);
        verify(correo).enviar(c.capture());
        assertThat(c.getValue().para()).isEqualTo("ana@x.com");
        assertThat(c.getValue().texto()).contains("La Esperanza / Lote 1 (Uchuva · Colombia)", "hoy toca fumigar");
        verify(alertas, never()).alertasDelDia(3L, hoy);
    }

    @Test
    @DisplayName("respeta a quien apagó el resumen")
    void preferenciaApagada() {
        when(preferencias.obtener(1L)).thenReturn(new PreferenciasCorreo(1L, false, true));
        when(alertas.alertasDelDia(2L, hoy)).thenReturn(List.of(fumigarHoy()));
        assertThat(service.enviarResumenDelDia(false)).isEqualTo(1);
        verify(alertas, never()).alertasDelDia(1L, hoy);
    }

    @Test
    @DisplayName("la campana recibe el resumen aunque el correo esté apagado; nunca al administrador")
    void bandeja() {
        var bandeja = mock(com.agroruta.notification.application.ports.in.BandejaAvisosUseCase.class);
        service.setBandeja(bandeja);
        when(preferencias.obtener(1L)).thenReturn(new PreferenciasCorreo(1L, false, true));
        when(alertas.alertasDelDia(1L, hoy)).thenReturn(List.of(fumigarHoy()));
        when(alertas.alertasDelDia(2L, hoy)).thenReturn(List.of());

        assertThat(service.enviarResumenDelDia(false)).isZero(); // ningún correo: Ana lo apagó y Luis no tiene nada

        var c = ArgumentCaptor.forClass(com.agroruta.notification.domain.AvisoBandeja.Nuevo.class);
        verify(bandeja).avisar(c.capture());
        assertThat(c.getValue().usuarioId()).isEqualTo(1L);
        assertThat(c.getValue().tipo()).isEqualTo(com.agroruta.notification.domain.AvisoBandeja.Tipo.RESUMEN_DIA);
        assertThat(c.getValue().datos()).containsEntry("alertas", 1).containsEntry("fecha", "2026-11-05");
        assertThat(c.getValue().etiqueta()).isEqualTo("resumen-2026-11-05");
        verify(correo, never()).enviar(any());
        verify(alertas, never()).alertasDelDia(3L, hoy);
    }

    @Test
    @DisplayName("un reintento el mismo día no duplica correos, salvo que se fuerce")
    void noRepite() {
        when(alertas.alertasDelDia(1L, hoy)).thenReturn(List.of(fumigarHoy()));
        service.enviarResumenDelDia(false);
        assertThat(service.enviarResumenDelDia(false)).isZero();
        assertThat(service.enviarResumenDelDia(true)).isEqualTo(1);
        verify(correo, times(2)).enviar(any());
    }

    @Test
    @DisplayName("helada en una finca con ubicación: manda aunque no haya otras alertas")
    void soloClima() {
        Finca f = new Finca(10L, "El Alto", "Tunja", 2.0, 2L);
        f.actualizarCentroide(5.5, -73.3);
        when(fincas.findByAgricultorId(2L)).thenReturn(List.of(f));
        when(clima.getPronostico24h(anyDouble(), anyDouble())).thenReturn(new Pronostico(1.5, 18, 0, 2));

        assertThat(service.enviarResumenDelDia(false)).isEqualTo(1);
        ArgumentCaptor<Correo> c = ArgumentCaptor.forClass(Correo.class);
        verify(correo).enviar(c.capture());
        assertThat(c.getValue().texto()).contains("El Alto", "riesgo de helada");
    }

    @Test
    @DisplayName("si el clima falla para una finca, sigue con las alertas del cultivo")
    void climaFalla() {
        Finca f = new Finca(10L, "El Alto", "Tunja", 2.0, 1L);
        f.actualizarCentroide(5.5, -73.3);
        when(fincas.findByAgricultorId(1L)).thenReturn(List.of(f));
        when(clima.getPronostico24h(anyDouble(), anyDouble())).thenThrow(new RuntimeException("sin red"));
        when(alertas.alertasDelDia(1L, hoy)).thenReturn(List.of(fumigarHoy()));
        assertThat(service.enviarResumenDelDia(false)).isEqualTo(1);
    }
}
