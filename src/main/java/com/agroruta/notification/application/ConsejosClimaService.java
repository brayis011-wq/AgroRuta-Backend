package com.agroruta.notification.application;

import com.agroruta.crop.application.ports.in.AlertasCultivoUseCase;
import com.agroruta.crop.application.ports.out.FincaRepository;
import com.agroruta.crop.domain.AlertaCultivo;
import com.agroruta.crop.domain.Finca;
import com.agroruta.notification.application.ports.in.ConsejosClimaUseCase;
import com.agroruta.weather.application.ports.in.WeatherUseCase;
import com.agroruta.weather.domain.ConsejoClima;
import com.agroruta.weather.domain.ConsejoClima.Nivel;
import com.agroruta.weather.domain.ConsejoClima.Tipo;
import com.agroruta.weather.domain.FranjaClima;
import com.agroruta.weather.domain.Pronostico;
import com.agroruta.weather.domain.VentanaFumigacion;
import com.agroruta.weather.domain.VentanaFumigacion.Estado;
import com.agroruta.weather.domain.VentanaFumigacion.Resultado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ConsejosClimaService implements ConsejosClimaUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConsejosClimaService.class);

    private final FincaRepository fincas;
    private final AlertasCultivoUseCase alertasCultivo;
    private final WeatherUseCase clima;
    private final Clock reloj;

    @Autowired
    public ConsejosClimaService(FincaRepository fincas, AlertasCultivoUseCase alertasCultivo, WeatherUseCase clima) {
        this(fincas, alertasCultivo, clima, Clock.system(ZoneId.of("America/Bogota")));
    }

    ConsejosClimaService(FincaRepository fincas, AlertasCultivoUseCase alertasCultivo, WeatherUseCase clima, Clock reloj) {
        this.fincas = fincas;
        this.alertasCultivo = alertasCultivo;
        this.clima = clima;
        this.reloj = reloj;
    }

    @Override
    public List<ConsejoClima> consejos(Long agricultorId) {
        List<ConsejoClima> res = new ArrayList<>();
        if (agricultorId == null) return res;
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDate hoy = ahora.toLocalDate();
        List<AlertaCultivo> agenda = alertasCultivo.alertasDelDia(agricultorId, hoy);

        for (Finca finca : fincas.findByAgricultorId(agricultorId)) {
            if (!finca.tieneCentroide()) continue;
            List<FranjaClima> franjas;
            try {
                franjas = clima.getFranjas48h(finca.getCentroideLat(), finca.getCentroideLng());
            } catch (RuntimeException e) {
                log.warn("Sin pronóstico para la finca {}: {}", finca.getId(), e.getMessage());
                continue;
            }
            deLaFinca(finca, franjas, ahora).forEach(res::add);
            for (AlertaCultivo a : agenda) {
                // Las alertas traen el nombre de la finca; dos fincas del mismo agricultor no pueden llamarse igual
                if (!finca.getNombre().equals(a.finca())) continue;
                deLaTarea(finca, a, franjas, ahora).ifPresent(res::add);
            }
        }
        res.sort(Comparator.comparing((ConsejoClima c) -> c.nivel().ordinal())
                .thenComparing(ConsejoClima::dia, Comparator.nullsLast(Comparator.naturalOrder())));
        return res;
    }

    /** Fumigación o riego programados, vistos con el pronóstico */
    private static Optional<ConsejoClima> deLaTarea(Finca finca, AlertaCultivo a, List<FranjaClima> franjas, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        if (a.esFumigacionPendiente()) {
            LocalDate dia = a.tipo() == AlertaCultivo.Tipo.FUMIGACION_MANANA ? hoy.plusDays(1) : hoy;
            Optional<Resultado> r = VentanaFumigacion.para(franjas, ahora, dia);
            // Ya es de noche: lo que importa es cómo viene mañana
            if (r.isEmpty() && dia.equals(hoy)) {
                dia = hoy.plusDays(1);
                r = VentanaFumigacion.para(franjas, ahora, dia);
            }
            LocalDate elDia = dia;
            return r.map(x -> new ConsejoClima(
                    x.estado() == Estado.NO ? Tipo.NO_FUMIGAR : x.estado() == Estado.VENTANA ? Tipo.FUMIGAR_EN_VENTANA : Tipo.FUMIGAR_BIEN,
                    x.estado() == Estado.NO ? Nivel.ALERTA : x.estado() == Estado.VENTANA ? Nivel.PRECAUCION : Nivel.BIEN,
                    finca.getId(), finca.getNombre(), a.lote(), a.cultivo(), elDia,
                    x.motivo() == null ? null : x.motivo().name(), x.problemaDesde(), x.ventanaInicio(), x.ventanaFin(), null));
        }
        if (a.tipo() == AlertaCultivo.Tipo.LABOR_HOY && "RIEGO".equals(a.titulo())) {
            double lluvia = franjas.stream()
                    .filter(f -> f.inicio().toLocalDate().equals(hoy) && f.fin().isAfter(ahora))
                    .mapToDouble(FranjaClima::lluviaMm).sum();
            if (lluvia >= Pronostico.LLUVIA_LAVA_PRODUCTO_MM) {
                LocalDateTime desde = franjas.stream()
                        .filter(f -> f.inicio().toLocalDate().equals(hoy) && f.fin().isAfter(ahora) && f.llueve())
                        .map(FranjaClima::inicio).findFirst().orElse(null);
                return Optional.of(new ConsejoClima(Tipo.NO_REGAR, Nivel.PRECAUCION, finca.getId(), finca.getNombre(),
                        a.lote(), a.cultivo(), hoy, null, desde, null, null, lluvia));
            }
        }
        return Optional.empty();
    }

    /** Helada o lluvia fuerte en las próximas 24 horas: afectan a toda la finca, haya o no tareas */
    private static List<ConsejoClima> deLaFinca(Finca finca, List<FranjaClima> franjas, LocalDateTime ahora) {
        List<ConsejoClima> res = new ArrayList<>();
        List<FranjaClima> dia = franjas.stream()
                .filter(f -> f.fin().isAfter(ahora) && f.inicio().isBefore(ahora.plusHours(24))).toList();
        dia.stream().min(Comparator.comparingDouble(FranjaClima::temp))
                .filter(f -> f.temp() <= Pronostico.TEMP_HELADA)
                .ifPresent(f -> res.add(new ConsejoClima(Tipo.HELADA, Nivel.ALERTA, finca.getId(), finca.getNombre(),
                        null, null, f.inicio().toLocalDate(), null, f.inicio(), null, null, f.temp())));
        double lluvia = dia.stream().mapToDouble(FranjaClima::lluviaMm).sum();
        if (lluvia >= Pronostico.LLUVIA_FUERTE_MM) {
            LocalDateTime desde = dia.stream().filter(FranjaClima::llueve).map(FranjaClima::inicio).findFirst().orElse(null);
            res.add(new ConsejoClima(Tipo.LLUVIA_FUERTE, Nivel.PRECAUCION, finca.getId(), finca.getNombre(),
                    null, null, ahora.toLocalDate(), null, desde, null, null, lluvia));
        }
        return res;
    }
}
