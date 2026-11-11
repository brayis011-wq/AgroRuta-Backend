package com.agroruta.user.application;

import com.agroruta.shared.evento.ContrasenaCambiada;
import com.agroruta.shared.exception.SesionInvalidaException;
import com.agroruta.user.application.ports.in.SesionUseCase;
import com.agroruta.user.application.ports.out.SesionRepository;
import com.agroruta.user.application.ports.out.UsuarioRepository;
import com.agroruta.user.domain.Sesion;
import com.agroruta.user.domain.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class SesionService implements SesionUseCase, com.agroruta.user.application.ports.in.SesionesAbiertasUseCase {

    private static final Logger log = LoggerFactory.getLogger(SesionService.class);

    /** Margen para que dos pestañas renueven con el mismo token sin que parezca un robo. */
    static final Duration MARGEN_ROTACION = Duration.ofSeconds(60);

    private static final String TERMINO = "Tu sesión terminó. Vuelve a iniciar sesión.";

    private final SesionRepository sesiones;
    private final UsuarioRepository usuarios;
    private final Duration duracion;
    private final Clock reloj;
    private final SecureRandom azar = new SecureRandom();
    private final com.agroruta.user.application.ports.out.DispositivoPort dispositivo;

    @Autowired
    public SesionService(SesionRepository sesiones, UsuarioRepository usuarios,
                         com.agroruta.user.application.ports.out.DispositivoPort dispositivo,
                         @Value("${app.sesion.duracion-dias:30}") long duracionDias) {
        this(sesiones, usuarios, Duration.ofDays(duracionDias), Clock.systemDefaultZone(), dispositivo);
    }

    SesionService(SesionRepository sesiones, UsuarioRepository usuarios, Duration duracion, Clock reloj) {
        this(sesiones, usuarios, duracion, reloj, () -> null);
    }

    SesionService(SesionRepository sesiones, UsuarioRepository usuarios, Duration duracion, Clock reloj,
                  com.agroruta.user.application.ports.out.DispositivoPort dispositivo) {
        this.sesiones = sesiones;
        this.usuarios = usuarios;
        this.duracion = duracion;
        this.reloj = reloj;
        this.dispositivo = dispositivo;
    }

    @Override
    @Transactional
    public String abrir(Long usuarioId) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        sesiones.borrarVencidas(usuarioId, ahora);
        String token = nuevoToken();
        sesiones.guardar(Sesion.nueva(usuarioId, hash(token), ahora, duracion, dispositivo.actual()));
        return token;
    }

    // noRollbackFor: si se detecta un token robado, el cierre de todas las sesiones debe quedar guardado
    @Override
    @Transactional(noRollbackFor = SesionInvalidaException.class)
    public Renovacion renovar(String token) {
        if (token == null || token.isBlank()) throw new SesionInvalidaException(TERMINO);
        LocalDateTime ahora = LocalDateTime.now(reloj);

        Sesion sesion = sesiones.buscarPorTokenHash(hash(token))
                .orElseThrow(() -> new SesionInvalidaException(TERMINO));
        if (sesion.revocada() || sesion.vencida(ahora)) throw new SesionInvalidaException(TERMINO);

        if (sesion.rotada() && !sesion.rotadaHaceMenosDe(MARGEN_ROTACION, ahora)) {
            log.warn("Token de sesión reutilizado para el usuario {}: se cierran todas sus sesiones", sesion.getUsuarioId());
            sesiones.revocarTodas(sesion.getUsuarioId(), ahora);
            throw new SesionInvalidaException(TERMINO);
        }

        Usuario usuario = usuarios.findById(sesion.getUsuarioId())
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new SesionInvalidaException(TERMINO));

        sesion.rotar(ahora);
        sesiones.guardar(sesion);
        String nuevo = nuevoToken();
        sesiones.guardar(sesion.siguiente(hash(nuevo), ahora, duracion));
        return new Renovacion(usuario, nuevo);
    }

    @Override
    @Transactional
    public void cerrar(String token) {
        if (token == null || token.isBlank()) return;
        LocalDateTime ahora = LocalDateTime.now(reloj);
        sesiones.buscarPorTokenHash(hash(token)).ifPresent(s -> {
            s.revocar(ahora);
            sesiones.guardar(s);
        });
    }

    // ── Sesiones abiertas ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.agroruta.user.domain.SesionAbierta> listar(String token) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Sesion actual = vigente(token, ahora);
        // Dos pestañas que renovaron a la vez dejan dos filas abiertas de la misma familia: se muestra una
        java.util.Map<String, Sesion> porFamilia = new java.util.LinkedHashMap<>();
        for (Sesion s : sesiones.buscarAbiertas(actual.getUsuarioId(), ahora)) {
            porFamilia.merge(familiaDe(s), s, (a, b) -> a.getCreadoEn().isBefore(b.getCreadoEn()) ? b : a);
        }
        String miFamilia = familiaDe(actual);
        return porFamilia.values().stream()
                .map(s -> new com.agroruta.user.domain.SesionAbierta(s.getId(), s.getDispositivo(),
                        instante(s.getIniciadaEn()), instante(s.getCreadoEn()), familiaDe(s).equals(miFamilia)))
                .sorted(java.util.Comparator
                        .comparing((com.agroruta.user.domain.SesionAbierta s) -> !s.actual())
                        .thenComparing(com.agroruta.user.domain.SesionAbierta::ultimoUso, java.util.Comparator.reverseOrder()))
                .toList();
    }

    @Override
    @Transactional
    public boolean cerrar(String token, Long sesionId) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Sesion actual = vigente(token, ahora);
        // Solo entre las sesiones del dueño del token: nadie cierra la sesión de otra persona
        Sesion objetivo = sesiones.buscarAbiertas(actual.getUsuarioId(), ahora).stream()
                .filter(s -> s.getId().equals(sesionId)).findFirst()
                .orElseThrow(() -> new com.agroruta.shared.exception.ResourceNotFoundException("Esa sesión ya no está abierta."));
        cerrarFamilia(objetivo, ahora);
        return familiaDe(objetivo).equals(familiaDe(actual));
    }

    @Override
    @Transactional
    public int cerrarOtras(String token) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Sesion actual = vigente(token, ahora);
        java.util.Set<String> otras = new java.util.HashSet<>();
        for (Sesion s : sesiones.buscarAbiertas(actual.getUsuarioId(), ahora)) {
            if (!familiaDe(s).equals(familiaDe(actual))) otras.add(familiaDe(s));
        }
        if (actual.getFamilia() != null) {
            sesiones.revocarOtras(actual.getUsuarioId(), actual.getFamilia(), ahora);
        } else {
            for (Sesion s : sesiones.buscarAbiertas(actual.getUsuarioId(), ahora)) {
                if (!s.getId().equals(actual.getId())) cerrarFamilia(s, ahora);
            }
        }
        return otras.size();
    }

    /** La sesión de la cookie, si todavía sirve */
    private Sesion vigente(String token, LocalDateTime ahora) {
        if (token == null || token.isBlank()) throw new SesionInvalidaException(TERMINO);
        Sesion s = sesiones.buscarPorTokenHash(hash(token)).orElseThrow(() -> new SesionInvalidaException(TERMINO));
        // Recién renovada por otra pestaña: sigue siendo la misma persona
        boolean recienRotada = s.rotada() && s.rotadaHaceMenosDe(MARGEN_ROTACION, ahora);
        if (s.revocada() || s.vencida(ahora) || (s.rotada() && !recienRotada)) throw new SesionInvalidaException(TERMINO);
        return s;
    }

    private void cerrarFamilia(Sesion s, LocalDateTime ahora) {
        if (s.getFamilia() != null) {
            sesiones.revocarFamilia(s.getUsuarioId(), s.getFamilia(), ahora);
        } else {
            s.revocar(ahora);
            sesiones.guardar(s);
        }
    }

    /** Las filas anteriores a las familias cuentan cada una como su propio inicio de sesión */
    private static String familiaDe(Sesion s) {
        return s.getFamilia() != null ? s.getFamilia() : "v" + s.getId();
    }

    private java.time.Instant instante(LocalDateTime t) {
        return t == null ? null : t.atZone(reloj.getZone()).toInstant();
    }

    /** Cambió la contraseña (desde el perfil o con el enlace del correo): se cierran todas las sesiones. */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void contrasenaCambiada(ContrasenaCambiada e) {
        sesiones.revocarTodas(e.usuarioId(), LocalDateTime.now(reloj));
    }

    private String nuevoToken() {
        byte[] b = new byte[32];
        azar.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    static String hash(String token) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
