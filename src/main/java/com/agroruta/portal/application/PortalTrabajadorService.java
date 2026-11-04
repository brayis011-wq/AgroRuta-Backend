package com.agroruta.portal.application;

import com.agroruta.portal.application.ports.in.PortalTrabajadorUseCase;
import com.agroruta.portal.domain.EstadoPortal;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.user.application.ports.out.VinculoTrabajadorPort;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.domain.UsuarioRepository;
import com.agroruta.worker.application.ports.in.JornalUseCase;
import com.agroruta.worker.application.ports.in.NominaUseCase;
import com.agroruta.worker.application.ports.in.PagoUseCase;
import com.agroruta.worker.domain.EstadoAccesoPortal;
import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.Jornal;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.Pago;
import com.agroruta.worker.domain.Trabajador;
import com.agroruta.worker.domain.TrabajadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PortalTrabajadorService implements PortalTrabajadorUseCase, VinculoTrabajadorPort {

    private final TrabajadorRepository trabajadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final JornalUseCase jornalUseCase;
    private final NominaUseCase nominaUseCase;
    private final PagoUseCase pagoUseCase;

    public PortalTrabajadorService(TrabajadorRepository trabajadorRepository,
                                   UsuarioRepository usuarioRepository,
                                   JornalUseCase jornalUseCase,
                                   NominaUseCase nominaUseCase,
                                   PagoUseCase pagoUseCase) {
        this.trabajadorRepository = trabajadorRepository;
        this.usuarioRepository = usuarioRepository;
        this.jornalUseCase = jornalUseCase;
        this.nominaUseCase = nominaUseCase;
        this.pagoUseCase = pagoUseCase;
    }

    // ── Vínculo (lo usa también el registro) ─────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public void validarCedulaDisponible(String cedula) {
        Trabajador t = buscarPorCedula(cedula);
        if (estaOcupado(t, null)) {
            throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "Esta cédula ya tiene una cuenta vinculada o una solicitud en curso.");
        }
    }

    @Override
    public void solicitarAcceso(Long usuarioId, String nombre, String email, String cedula) {
        Trabajador destino = buscarPorCedula(cedula);
        if (estaOcupado(destino, usuarioId)) {
            throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "Esta cédula ya tiene una cuenta vinculada o una solicitud en curso.");
        }

        // Si esta cuenta tenía otra solicitud (rechazada o pendiente), se libera
        trabajadorRepository.buscarPorUsuarioId(usuarioId)
                .filter(anterior -> !anterior.getId().equals(destino.getId()))
                .ifPresent(anterior -> {
                    if (anterior.tieneAccesoAprobado()) {
                        throw new BusinessException(ErrorCode.INVALID_OPERATION,
                                "Tu cuenta ya está vinculada a un trabajador.");
                    }
                    anterior.liberarAcceso();
                    trabajadorRepository.guardar(anterior);
                });

        destino.solicitarAcceso(usuarioId, nombre, email);
        trabajadorRepository.guardar(destino);
    }

    // ── Portal ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public EstadoPortal estado(Long usuarioId) {
        Optional<Trabajador> vinculado = trabajadorRepository.buscarPorUsuarioId(usuarioId);
        if (vinculado.isEmpty() || vinculado.get().getAccesoEstado() == null) {
            return new EstadoPortal(EstadoPortal.SIN_SOLICITUD, null, null);
        }
        Trabajador t = vinculado.get();
        if (!t.tieneAccesoAprobado()) {
            return new EstadoPortal(t.getAccesoEstado().name(), null, null);
        }
        String empleador = t.getAgricultorId() == null ? null
                : usuarioRepository.findById(t.getAgricultorId()).map(Usuario::getNombre).orElse(null);
        return new EstadoPortal(EstadoAccesoPortal.APROBADO.name(), t, empleador);
    }

    @Override
    public EstadoPortal solicitar(Long usuarioId, String cedula) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));
        solicitarAcceso(usuarioId, usuario.getNombre(), usuario.getEmail(), cedula);
        return estado(usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Jornal> misJornales(Long usuarioId) {
        return jornalUseCase.listarPorTrabajador(trabajadorAprobado(usuarioId).getId()).stream()
                .sorted(Comparator.comparing(Jornal::getFecha, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Nomina> misNominas(Long usuarioId) {
        return nominaUseCase.listarPorTrabajador(trabajadorAprobado(usuarioId).getId()).stream()
                .filter(n -> n.getEstado() != EstadoNomina.ANULADA)
                .sorted(Comparator.comparing(Nomina::getPeriodoInicio, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pago> misPagos(Long usuarioId) {
        return pagoUseCase.historialPorTrabajador(trabajadorAprobado(usuarioId).getId()).stream()
                .sorted(Comparator.comparing(Pago::getFechaPago, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    // ── Auxiliares ──────────────────────────────────────────────────────────

    /** Quita puntos, espacios y guiones: "1.052.334-120" → "1052334120". */
    static String normalizar(String cedula) {
        return cedula == null ? "" : cedula.replaceAll("[.\\s-]", "").toUpperCase();
    }

    private Trabajador buscarPorCedula(String cedula) {
        String normalizada = normalizar(cedula);
        if (normalizada.isEmpty()) {
            throw new IllegalArgumentException("La cédula es obligatoria.");
        }
        return trabajadorRepository.buscarPorCedulaNormalizada(normalizada)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_OPERATION,
                        "Ningún agricultor ha registrado esta cédula. Pídele a tu empleador que te registre primero."));
    }

    /** Ya hay otra cuenta usando a este trabajador (pendiente o aprobada). */
    private static boolean estaOcupado(Trabajador t, Long usuarioId) {
        boolean enUso = t.getUsuarioId() != null
                && (t.getAccesoEstado() == EstadoAccesoPortal.PENDIENTE || t.getAccesoEstado() == EstadoAccesoPortal.APROBADO);
        return enUso && !t.getUsuarioId().equals(usuarioId);
    }

    private Trabajador trabajadorAprobado(Long usuarioId) {
        return trabajadorRepository.buscarPorUsuarioId(usuarioId)
                .filter(Trabajador::tieneAccesoAprobado)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED,
                        "Tu acceso todavía no ha sido aprobado por tu empleador."));
    }
}
