package com.agroruta.notification.application;

import com.agroruta.crop.application.ports.in.AlertasCultivoUseCase;
import com.agroruta.crop.domain.AlertaCultivo;
import com.agroruta.crop.domain.Finca;
import com.agroruta.crop.domain.FincaRepository;
import com.agroruta.inventario.application.ports.in.StockBajoUseCase;
import com.agroruta.inventario.domain.StockInsumo;
import com.agroruta.notification.application.MensajesCorreo.ClimaFinca;
import com.agroruta.notification.application.ports.in.AlertasDiariasUseCase;
import com.agroruta.notification.application.ports.in.PreferenciasCorreoUseCase;
import com.agroruta.notification.application.ports.out.CorreoPort;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.domain.UsuarioRepository;
import com.agroruta.weather.application.port.in.WeatherUseCase;
import com.agroruta.weather.domain.Pronostico;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * El resumen de cada mañana para los agricultores: fumigaciones, labores, reentrada, carencia
 * clima de sus fincas e insumos que se están acabando. Solo se manda a quien tiene algo para hoy.
 *
 * Lo dispara una tarea de GitHub Actions (Render gratis se duerme y no correría un @Scheduled).
 */
@Service
public class AlertasDiariasService implements AlertasDiariasUseCase {

    private static final Logger log = LoggerFactory.getLogger(AlertasDiariasService.class);

    private final UsuarioRepository usuarios;
    private final PreferenciasCorreoUseCase preferencias;
    private final AlertasCultivoUseCase alertasCultivo;
    private final FincaRepository fincas;
    private final WeatherUseCase clima;
    private final MensajesCorreo mensajes;
    private final CorreoPort correo;
    private final StockBajoUseCase stockBajo;
    private final Clock reloj;
    /** Último día que se mandó, para que un reintento de la tarea no duplique correos */
    private final AtomicReference<LocalDate> ultimoEnvio = new AtomicReference<>();

    @Autowired
    public AlertasDiariasService(UsuarioRepository usuarios, PreferenciasCorreoUseCase preferencias,
                                 AlertasCultivoUseCase alertasCultivo, FincaRepository fincas, WeatherUseCase clima,
                                 MensajesCorreo mensajes, CorreoPort correo, StockBajoUseCase stockBajo) {
        this(usuarios, preferencias, alertasCultivo, fincas, clima, mensajes, correo, stockBajo,
                Clock.system(ZoneId.of("America/Bogota")));
    }

    AlertasDiariasService(UsuarioRepository usuarios, PreferenciasCorreoUseCase preferencias,
                          AlertasCultivoUseCase alertasCultivo, FincaRepository fincas, WeatherUseCase clima,
                          MensajesCorreo mensajes, CorreoPort correo, StockBajoUseCase stockBajo, Clock reloj) {
        this.usuarios = usuarios;
        this.preferencias = preferencias;
        this.alertasCultivo = alertasCultivo;
        this.fincas = fincas;
        this.clima = clima;
        this.mensajes = mensajes;
        this.correo = correo;
        this.stockBajo = stockBajo;
        this.reloj = reloj;
    }

    @Override
    public int enviarResumenDelDia(boolean forzar) {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate anterior = ultimoEnvio.getAndSet(hoy);
        if (!forzar && hoy.equals(anterior)) {
            log.info("El resumen de hoy ({}) ya se mandó; no se repite", hoy);
            return 0;
        }
        int enviados = 0;
        for (Usuario u : usuarios.findAllActivos()) {
            if (u.getRol() != Rol.Agricultor || !preferencias.obtener(u.getId()).alertasCultivo()) continue;
            try {
                if (enviarA(u, hoy)) enviados++;
            } catch (RuntimeException e) {
                log.warn("No se pudo armar el resumen del agricultor {}", u.getId(), e);
            }
        }
        log.info("Resumen diario del {}: {} correos", hoy, enviados);
        return enviados;
    }

    private boolean enviarA(Usuario u, LocalDate hoy) {
        List<AlertaCultivo> alertas = alertasCultivo.alertasDelDia(u.getId(), hoy);
        List<ClimaFinca> tiempo = climaDe(u.getId(), alertas.stream().anyMatch(AlertaCultivo::esFumigacionPendiente));
        List<StockInsumo> stock = stockDe(u.getId());
        if (alertas.isEmpty() && tiempo.isEmpty() && stock.isEmpty()) return false;
        return correo.enviar(mensajes.resumenDiario(u.getNombre(), u.getEmail(), hoy, alertas, tiempo, stock));
    }

    /** Insumos que se están acabando; si falla, el resumen sale igual sin esa parte */
    private List<StockInsumo> stockDe(Long agricultorId) {
        try {
            return stockBajo.avisos(agricultorId);
        } catch (RuntimeException e) {
            log.warn("Sin avisos de bodega para el agricultor {}: {}", agricultorId, e.getMessage());
            return List.of();
        }
    }

    /** Solo las fincas con algo que avisar (helada, lluvia fuerte o lluvia el día que toca fumigar) */
    private List<ClimaFinca> climaDe(Long agricultorId, boolean hayFumigacion) {
        List<ClimaFinca> res = new ArrayList<>();
        for (Finca f : fincas.findByAgricultorId(agricultorId)) {
            if (!f.tieneCentroide()) continue;
            try {
                Pronostico p = clima.getPronostico24h(f.getCentroideLat(), f.getCentroideLng());
                if (p.riesgoHelada() || p.lluviaFuerte() || (hayFumigacion && p.lluviaLavaProducto())) {
                    res.add(new ClimaFinca(f.getNombre(), p));
                }
            } catch (RuntimeException e) {
                log.warn("Sin pronóstico para la finca {}: {}", f.getId(), e.getMessage());
            }
        }
        return res;
    }
}
