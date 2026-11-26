package com.agroruta.finance.application;

import com.agroruta.finance.application.ports.in.PreciosReferenciaUseCase;
import com.agroruta.finance.application.ports.out.PreciosReferenciaRepository;
import com.agroruta.finance.application.ports.out.SipsaPort;
import com.agroruta.finance.domain.BuscadorPrecio;
import com.agroruta.finance.domain.EstadoPrecios;
import com.agroruta.finance.domain.PrecioCultivo;
import com.agroruta.finance.domain.PrecioReferencia;
import com.agroruta.finance.domain.PreferenciaPrecio;
import com.agroruta.shared.exception.DatoInvalidoException;
import com.agroruta.shared.exception.ExternalApiException;
import com.agroruta.shared.util.Dinero;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PreciosReferenciaService implements PreciosReferenciaUseCase {

    private static final Logger log = LoggerFactory.getLogger(PreciosReferenciaService.class);

    /** Semanas que se guardan (para ver si el precio sube o baja) */
    static final int SEMANAS = 4;
    /** SIPSA publica una vez por semana: no vale la pena preguntar más seguido */
    static final Duration VIGENCIA = Duration.ofDays(3);
    /** Si falla, no se vuelve a intentar antes de esto (un fallo pasajero no deja la pantalla vacía medio día) */
    static final Duration ESPERA_TRAS_FALLO = Duration.ofMinutes(30);

    static final int MAX_ALTERNATIVAS = 8;

    private final PreciosReferenciaRepository repositorio;
    private final SipsaPort sipsa;
    private final Clock reloj;
    private final Executor segundoPlano;
    private final AtomicBoolean trayendo = new AtomicBoolean(false);
    private final AtomicReference<LocalDateTime> ultimoIntento = new AtomicReference<>();
    /** Por qué falló la última vez (null si la última salió bien o no se ha intentado) */
    private final AtomicReference<String> ultimoError = new AtomicReference<>();

    @Autowired
    public PreciosReferenciaService(PreciosReferenciaRepository repositorio, SipsaPort sipsa, Clock reloj) {
        this(repositorio, sipsa, reloj, Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "precios-sipsa");
            t.setDaemon(true);
            return t;
        }));
    }

    PreciosReferenciaService(
            PreciosReferenciaRepository repositorio, SipsaPort sipsa, Clock reloj, Executor segundoPlano) {
        this.repositorio = repositorio;
        this.sipsa = sipsa;
        this.reloj = reloj;
        this.segundoPlano = segundoPlano;
    }

    @Override
    public PrecioCultivo precioDe(Long agricultorId, String cultivo, String variedad) {
        actualizarSiHaceFalta();
        String nombre = cultivo == null ? "" : cultivo.trim();
        List<PrecioReferencia> semana = repositorio.ultimaSemana();
        List<PrecioReferencia> candidatos = BuscadorPrecio.candidatos(semana, nombre, variedad);
        List<PrecioReferencia> alternativas =
                candidatos.stream().limit(MAX_ALTERNATIVAS).toList();

        Optional<PreferenciaPrecio> pref = agricultorId == null || nombre.isEmpty()
                ? Optional.empty()
                : repositorio.preferencia(agricultorId, nombre);
        if (pref.isPresent() && pref.get().precioKg() != null) {
            return PrecioCultivo.propio(nombre, pref.get().precioKg(), alternativas);
        }
        if (pref.isPresent() && pref.get().producto() != null) {
            String elegido = pref.get().producto();
            Optional<PrecioReferencia> p = semana.stream()
                    .filter(x -> x.producto().equalsIgnoreCase(elegido))
                    .findFirst();
            if (p.isPresent()) return PrecioCultivo.deSipsa(nombre, p.get(), true, alternativas);
        }
        return candidatos.isEmpty()
                ? PrecioCultivo.sinPrecio(nombre, alternativas)
                : PrecioCultivo.deSipsa(nombre, candidatos.get(0), false, alternativas);
    }

    @Override
    public List<PrecioReferencia> buscar(String texto) {
        actualizarSiHaceFalta();
        return BuscadorPrecio.buscar(repositorio.ultimaSemana(), texto, 30);
    }

    @Override
    public List<PreferenciaPrecio> preferencias(Long agricultorId) {
        return agricultorId == null ? List.of() : repositorio.preferencias(agricultorId);
    }

    @Override
    public PreferenciaPrecio elegir(Long agricultorId, String cultivo, String producto, BigDecimal precioKg) {
        String nombre = cultivo == null ? "" : cultivo.trim();
        if (nombre.isEmpty()) throw new DatoInvalidoException("Indica el cultivo.");
        String prod = producto == null || producto.isBlank() ? null : producto.trim();
        if ((prod == null) == (precioKg == null)) {
            throw new DatoInvalidoException("Elige un producto de la lista o escribe tu propio precio por kilo.");
        }
        if (precioKg != null && precioKg.signum() <= 0) {
            throw new DatoInvalidoException("El precio por kilo debe ser mayor a 0.");
        }
        if (prod != null
                && repositorio.ultimaSemana().stream()
                        .noneMatch(p -> p.producto().equalsIgnoreCase(prod))) {
            throw new DatoInvalidoException("Ese producto no está en los precios de esta semana.");
        }
        PreferenciaPrecio p = new PreferenciaPrecio(nombre, prod, Dinero.redondear(precioKg));
        repositorio.guardarPreferencia(agricultorId, p, LocalDateTime.now(reloj));
        return p;
    }

    @Override
    public void quitarPreferencia(Long agricultorId, String cultivo) {
        if (cultivo != null) repositorio.borrarPreferencia(agricultorId, cultivo.trim());
    }

    @Override
    public int actualizar() {
        if (!trayendo.compareAndSet(false, true)) return 0;
        LocalDateTime ahora = LocalDateTime.now(reloj);
        ultimoIntento.set(ahora);
        try {
            List<PrecioReferencia> precios = sipsa.preciosRecientes(SEMANAS);
            if (precios.isEmpty()) {
                log.warn("SIPSA no devolvió precios");
                ultimoError.set("SIPSA no devolvió precios");
                return 0;
            }
            repositorio.guardar(precios, ahora);
            ultimoError.set(null);
            log.info("Precios de referencia de SIPSA actualizados: {} filas", precios.size());
            return precios.size();
        } catch (RuntimeException e) {
            log.warn("No se pudieron traer los precios de SIPSA: {}", e.getMessage());
            ultimoError.set(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            return 0;
        } finally {
            trayendo.set(false);
        }
    }

    @Override
    public EstadoPrecios cargarRespuesta(InputStream xml) {
        List<PrecioReferencia> precios;
        try {
            precios = sipsa.desdeRespuesta(xml, SEMANAS);
        } catch (ExternalApiException e) {
            throw new DatoInvalidoException("El archivo no es una respuesta de SIPSA: " + e.getMessage());
        }
        if (precios.isEmpty()) throw new DatoInvalidoException("El archivo no trae precios de SIPSA.");
        LocalDateTime ahora = LocalDateTime.now(reloj);
        repositorio.guardar(precios, ahora);
        ultimoError.set(null);
        log.info("Precios de SIPSA cargados desde un archivo: {} filas", precios.size());
        List<PrecioReferencia> semana = repositorio.ultimaSemana();
        return new EstadoPrecios(
                !semana.isEmpty(),
                semana.isEmpty() ? null : semana.get(0).semana(),
                ahora,
                trayendo.get(),
                ultimoIntento.get(),
                null);
    }

    @Override
    public EstadoPrecios estado() {
        actualizarSiHaceFalta();
        List<PrecioReferencia> semana = repositorio.ultimaSemana();
        return new EstadoPrecios(
                !semana.isEmpty(),
                semana.isEmpty() ? null : semana.get(0).semana(),
                repositorio.ultimaActualizacion().orElse(null),
                trayendo.get(),
                ultimoIntento.get(),
                ultimoError.get());
    }

    @Override
    public void actualizarSiHaceFalta() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime intento = ultimoIntento.get();
        if (intento != null && intento.plus(ESPERA_TRAS_FALLO).isAfter(ahora)) return;
        Optional<LocalDateTime> ultima = repositorio.ultimaActualizacion();
        if (ultima.isPresent() && ultima.get().plus(VIGENCIA).isAfter(ahora)) return;
        if (trayendo.get()) return;
        ultimoIntento.set(ahora);
        segundoPlano.execute(this::actualizar);
    }
}
