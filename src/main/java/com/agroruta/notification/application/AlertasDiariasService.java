package com.agroruta.notification.application;

import com.agroruta.crop.application.ports.in.AlertasCultivoUseCase;
import com.agroruta.crop.domain.AlertaCultivo;
import com.agroruta.crop.domain.Finca;
import com.agroruta.crop.application.ports.out.FincaRepository;
import com.agroruta.inventory.application.ports.in.StockBajoUseCase;
import com.agroruta.inventory.domain.StockInsumo;
import com.agroruta.notification.application.MensajesCorreo.ClimaFinca;
import com.agroruta.notification.application.ports.in.AlertasDiariasUseCase;
import com.agroruta.notification.application.ports.in.PreferenciasCorreoUseCase;
import com.agroruta.notification.application.ports.out.CorreoPort;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.application.ports.out.UsuarioRepository;
import com.agroruta.weather.application.ports.in.WeatherUseCase;
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
    /** null en pruebas antiguas: el resumen sale sin los consejos por horas */
    private final com.agroruta.notification.application.ports.in.ConsejosClimaUseCase consejosClima;
    /** null si no hay avisos push (pruebas): el resumen solo sale por correo */
    private com.agroruta.notification.application.ports.in.AvisosPushUseCase push;
    /** Último día que se mandó, para que un reintento de la tarea no duplique correos */
    private final AtomicReference<LocalDate> ultimoEnvio = new AtomicReference<>();

    @Autowired
    public AlertasDiariasService(UsuarioRepository usuarios, PreferenciasCorreoUseCase preferencias,
                                 AlertasCultivoUseCase alertasCultivo, FincaRepository fincas, WeatherUseCase clima,
                                 MensajesCorreo mensajes, CorreoPort correo, StockBajoUseCase stockBajo,
                                 com.agroruta.notification.application.ports.in.ConsejosClimaUseCase consejosClima) {
        this(usuarios, preferencias, alertasCultivo, fincas, clima, mensajes, correo, stockBajo,
                Clock.system(ZoneId.of("America/Bogota")), consejosClima);
    }

    AlertasDiariasService(UsuarioRepository usuarios, PreferenciasCorreoUseCase preferencias,
                          AlertasCultivoUseCase alertasCultivo, FincaRepository fincas, WeatherUseCase clima,
                          MensajesCorreo mensajes, CorreoPort correo, StockBajoUseCase stockBajo, Clock reloj) {
        this(usuarios, preferencias, alertasCultivo, fincas, clima, mensajes, correo, stockBajo, reloj, null);
    }

    AlertasDiariasService(UsuarioRepository usuarios, PreferenciasCorreoUseCase preferencias,
                          AlertasCultivoUseCase alertasCultivo, FincaRepository fincas, WeatherUseCase clima,
                          MensajesCorreo mensajes, CorreoPort correo, StockBajoUseCase stockBajo, Clock reloj,
                          com.agroruta.notification.application.ports.in.ConsejosClimaUseCase consejosClima) {
        this.consejosClima = consejosClima;
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

    @Autowired(required = false)
    void setPush(com.agroruta.notification.application.ports.in.AvisosPushUseCase push) {
        this.push = push;
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
            if (u.getRol() != Rol.Agricultor) continue;
            // El correo se puede apagar en Configuración; el aviso push sale si activó los avisos en algún dispositivo
            boolean porCorreo = preferencias.obtener(u.getId()).alertasCultivo();
            boolean porPush = push != null && push.tieneSuscripcion(u.getId());
            if (!porCorreo && !porPush) continue;
            try {
                if (enviarA(u, hoy, porCorreo, porPush)) enviados++;
            } catch (RuntimeException e) {
                log.warn("No se pudo armar el resumen del agricultor {}", u.getId(), e);
            }
        }
        log.info("Resumen diario del {}: {} correos", hoy, enviados);
        return enviados;
    }

    private boolean enviarA(Usuario u, LocalDate hoy, boolean porCorreo, boolean porPush) {
        List<AlertaCultivo> alertas = alertasCultivo.alertasDelDia(u.getId(), hoy);
        List<ClimaFinca> tiempo = climaDe(u.getId(), alertas.stream().anyMatch(AlertaCultivo::esFumigacionPendiente));
        List<StockInsumo> stock = stockDe(u.getId());
        List<com.agroruta.weather.domain.ConsejoClima> consejos = consejosDe(u.getId());
        if (alertas.isEmpty() && tiempo.isEmpty() && stock.isEmpty() && consejos.isEmpty()) return false;
        if (porPush) {
            push.enviar(u.getId(), new com.agroruta.notification.domain.AvisoPush("Tu finca hoy",
                    resumenCorto(alertas, tiempo, stock, consejos, hoy), "/dashboard", "resumen-" + hoy));
        }
        return porCorreo && correo.enviar(mensajes.resumenDiario(u.getNombre(), u.getEmail(), hoy, alertas, tiempo, stock, consejos));
    }

    /** Lo esencial del día en una o dos líneas, para la notificación del teléfono */
    static String resumenCorto(List<AlertaCultivo> alertas, List<ClimaFinca> tiempo, List<StockInsumo> stock,
                               List<com.agroruta.weather.domain.ConsejoClima> consejos, LocalDate hoy) {
        List<String> partes = new ArrayList<>();
        // Primero lo que cambia el plan del día: no fumigar, helada, lluvia fuerte
        consejos.stream()
                .filter(c -> c.nivel() == com.agroruta.weather.domain.ConsejoClima.Nivel.ALERTA)
                .findFirst()
                .or(() -> consejos.stream().findFirst())
                .ifPresent(c -> {
                    String texto = MensajesCorreo.consejo(c, hoy);
                    String donde = c.lote() != null ? c.lote() : c.finca();
                    partes.add((donde == null ? "" : donde + ": ") + texto.replaceAll("\\.$", ""));
                });
        if (consejos.isEmpty() && !tiempo.isEmpty()) {
            partes.add("Alerta de clima en " + tiempo.get(0).finca());
        }
        if (!alertas.isEmpty()) {
            partes.add(alertas.size() == 1 ? "1 aviso del cultivo" : alertas.size() + " avisos del cultivo");
        }
        if (!stock.isEmpty()) {
            partes.add(stock.size() == 1 ? "1 insumo por acabarse" : stock.size() + " insumos por acabarse");
        }
        String texto = String.join(" · ", partes);
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    /** "No fumigues hoy: llueve a las 3"; si falla, el resumen sale igual sin esa parte */
    private List<com.agroruta.weather.domain.ConsejoClima> consejosDe(Long agricultorId) {
        if (consejosClima == null) return List.of();
        try {
            // Un "todo bien para fumigar" no justifica un correo por sí solo, pero sí acompaña a los demás avisos
            return consejosClima.consejos(agricultorId);
        } catch (RuntimeException e) {
            log.warn("Sin consejos de clima para el agricultor {}: {}", agricultorId, e.getMessage());
            return List.of();
        }
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
