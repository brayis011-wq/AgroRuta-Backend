package com.agroruta.user.application;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.FalloInternoException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.shared.util.QrSvg;
import com.agroruta.user.application.ports.in.SegundoFactorUseCase;
import com.agroruta.user.application.ports.out.SegundoFactorRepository;
import com.agroruta.user.application.ports.out.UsuarioRepository;
import com.agroruta.user.domain.SegundoFactor;
import com.agroruta.user.domain.Totp;
import com.agroruta.user.domain.Usuario;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SegundoFactorService implements SegundoFactorUseCase {

    static final String EMISOR = "AgroRuta";
    static final int CODIGOS_RESPALDO = 8;
    /** Sin 0, O, 1 ni I: no se confunden al copiarlos a mano */
    private static final String LETRAS_RESPALDO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SegundoFactorRepository repo;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final SecretKeySpec claveCifrado;
    /** Para firmar las marcas de dispositivo recordado; distinta de la de cifrado */
    private final byte[] claveDispositivo;

    private final Clock reloj;
    private final SecureRandom azar = new SecureRandom();

    public SegundoFactorService(
            SegundoFactorRepository repo,
            UsuarioRepository usuarios,
            PasswordEncoder passwordEncoder,
            @Value("${app.segundo-factor.clave:${jwt.secret}}") String clave,
            Clock reloj) {
        this.repo = repo;
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.reloj = reloj;
        // Clave propia para cifrar los secretos, derivada del secreto del servidor
        this.claveCifrado = new SecretKeySpec(
                hmacSha256(clave.getBytes(StandardCharsets.UTF_8), "agroruta/segundo-factor/v1"), "AES");
        this.claveDispositivo = hmacSha256(clave.getBytes(StandardCharsets.UTF_8), "agroruta/dispositivo-recordado/v1");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean activo(Long usuarioId) {
        return usuarioId != null
                && repo.buscar(usuarioId).map(SegundoFactor::isActivo).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Estado estado(Long usuarioId) {
        return repo.buscar(usuarioId)
                .filter(SegundoFactor::isActivo)
                .map(s -> new Estado(true, s.getRespaldo().size()))
                .orElse(new Estado(false, 0));
    }

    @Override
    @Transactional
    public Preparacion preparar(Long usuarioId, String password) {
        Usuario usuario = usuarioCon(usuarioId, password);
        if (activo(usuarioId)) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "La verificación en dos pasos ya está activa.");
        }
        String secreto = Totp.nuevoSecreto(azar);
        repo.guardar(SegundoFactor.pendiente(usuarioId, cifrar(secreto), LocalDateTime.now(reloj)));
        String uri = Totp.uri(EMISOR, usuario.getEmail(), secreto);
        return new Preparacion(secreto, uri, QrSvg.dataUri(uri));
    }

    @Override
    @Transactional
    public List<String> activar(Long usuarioId, String codigo) {
        SegundoFactor sf = repo.buscar(usuarioId)
                .filter(s -> !s.isActivo())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_OPERATION, "Primero empieza la configuración para ver el código QR."));
        OptionalLong paso = Totp.verificar(descifrar(sf.getSecretoCifrado()), limpio(codigo), ahoraSegundos(), null);
        if (paso.isEmpty()) throw codigoIncorrecto();

        List<String> codigos = new ArrayList<>();
        List<String> hashes = new ArrayList<>();
        for (int i = 0; i < CODIGOS_RESPALDO; i++) {
            String c = nuevoCodigoRespaldo();
            codigos.add(c.substring(0, 5) + "-" + c.substring(5));
            hashes.add(hash(c));
        }
        sf.activar(paso.getAsLong(), hashes, LocalDateTime.now(reloj));
        repo.guardar(sf);
        return codigos;
    }

    @Override
    @Transactional
    public void desactivar(Long usuarioId, String password, String codigo) {
        usuarioCon(usuarioId, password);
        if (!activo(usuarioId)) return;
        if (!verificar(usuarioId, codigo)) throw codigoIncorrecto();
        repo.borrar(usuarioId);
    }

    @Override
    @Transactional
    public boolean verificar(Long usuarioId, String codigo) {
        SegundoFactor sf =
                repo.buscar(usuarioId).filter(SegundoFactor::isActivo).orElse(null);
        if (sf == null) return true;
        String c = limpio(codigo);
        if (c.isEmpty()) return false;

        if (c.matches("\\d{" + Totp.DIGITOS + "}")) {
            OptionalLong paso =
                    Totp.verificar(descifrar(sf.getSecretoCifrado()), c, ahoraSegundos(), sf.getUltimoPaso());
            if (paso.isEmpty()) return false;
            sf.usarPaso(paso.getAsLong());
            repo.guardar(sf);
            return true;
        }
        // No son 6 dígitos: puede ser un código de respaldo, que sirve una sola vez
        if (!sf.usarRespaldo(hash(c))) return false;
        repo.guardar(sf);
        return true;
    }

    // ── Dispositivo recordado ───────────────────────────────────────────────

    /** Formato: "usuarioId.venceEnSegundos.firma" */
    @Override
    @Transactional(readOnly = true)
    public Optional<String> recordarDispositivo(Long usuarioId) {
        if (usuarioId == null) return Optional.empty();
        SegundoFactor sf =
                repo.buscar(usuarioId).filter(SegundoFactor::isActivo).orElse(null);
        Usuario usuario = usuarios.findById(usuarioId).orElse(null);
        if (sf == null || usuario == null) return Optional.empty();
        long vence = reloj.instant().plus(RECORDAR_DISPOSITIVO).getEpochSecond();
        return Optional.of(usuarioId + "." + vence + "." + firma(usuarioId, vence, sf, usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean dispositivoRecordado(Long usuarioId, String marca) {
        if (usuarioId == null || marca == null || marca.isBlank()) return false;
        String[] p = marca.split("\\.");
        if (p.length != 3) return false;
        long id;
        long vence;
        try {
            id = Long.parseLong(p[0]);
            vence = Long.parseLong(p[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (id != usuarioId || vence <= ahoraSegundos()) return false;
        SegundoFactor sf =
                repo.buscar(usuarioId).filter(SegundoFactor::isActivo).orElse(null);
        Usuario usuario = usuarios.findById(usuarioId).orElse(null);
        if (sf == null || usuario == null) return false;
        byte[] esperada = firma(usuarioId, vence, sf, usuario).getBytes(StandardCharsets.US_ASCII);
        // Comparación en tiempo constante: no deja adivinar la firma letra por letra
        return MessageDigest.isEqual(esperada, p[2].getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * La firma cubre la fecha en que se activó el segundo factor y el hash de la contraseña:
     * si se vuelve a configurar o se cambia la contraseña, las marcas anteriores dejan de valer.
     */
    private String firma(Long usuarioId, long vence, SegundoFactor sf, Usuario usuario) {
        String datos = usuarioId + "|" + vence + "|" + sf.getActivadoEn() + "|" + usuario.getPassword();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacSha256(claveDispositivo, datos));
    }

    // ── Auxiliares ──────────────────────────────────────────────────────────

    private Usuario usuarioCon(Long usuarioId, String password) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));
        if (password == null || !passwordEncoder.matches(password, usuario.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "La contraseña no es correcta.");
        }
        return usuario;
    }

    private static BusinessException codigoIncorrecto() {
        return new BusinessException(
                ErrorCode.TWO_FACTOR_INVALID,
                "El código no es correcto. Revisa que la hora del celular esté bien y usa el código más reciente.");
    }

    /** Sin espacios ni guiones, en mayúsculas: "123 456" y "abcde-fghjk" se aceptan como se escriban */
    static String limpio(String codigo) {
        return codigo == null ? "" : codigo.replaceAll("[\\s-]", "").toUpperCase();
    }

    private long ahoraSegundos() {
        return reloj.instant().getEpochSecond();
    }

    private String nuevoCodigoRespaldo() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 10; i++) s.append(LETRAS_RESPALDO.charAt(azar.nextInt(LETRAS_RESPALDO.length())));
        return s.toString();
    }

    static String hash(String codigo) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(codigo.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new FalloInternoException(e);
        }
    }

    /** AES-256-GCM: base64(iv de 12 bytes + texto cifrado con su sello) */
    String cifrar(String texto) {
        try {
            byte[] iv = new byte[12];
            azar.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, claveCifrado, new GCMParameterSpec(128, iv));
            byte[] cifrado = c.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] todo = Arrays.copyOf(iv, iv.length + cifrado.length);
            System.arraycopy(cifrado, 0, todo, iv.length, cifrado.length);
            return Base64.getEncoder().encodeToString(todo);
        } catch (GeneralSecurityException e) {
            throw new FalloInternoException("No se pudo cifrar el secreto", e);
        }
    }

    String descifrar(String guardado) {
        try {
            byte[] todo = Base64.getDecoder().decode(guardado);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, claveCifrado, new GCMParameterSpec(128, todo, 0, 12));
            return new String(c.doFinal(todo, 12, todo.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // Pasa si cambió la clave del servidor: hay que borrar la fila de segundo_factor de esa cuenta
            throw new FalloInternoException("No se pudo leer el secreto del segundo factor", e);
        }
    }

    private static byte[] hmacSha256(byte[] clave, String datos) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            return mac.doFinal(datos.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new FalloInternoException(e);
        }
    }
}
