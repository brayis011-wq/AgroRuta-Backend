package com.agroruta.admin.application;

import com.agroruta.admin.application.ports.in.AdministrarSistemaUseCase;
import com.agroruta.admin.application.ports.out.AdminEstadisticasPort;
import com.agroruta.admin.domain.AgricultorDetalle;
import com.agroruta.admin.domain.AgricultorResumen;
import com.agroruta.admin.domain.ConteoAgricultor;
import com.agroruta.admin.domain.ResumenSistema;
import com.agroruta.crop.application.ports.in.FincaUseCase;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.domain.UsuarioRepository;
import com.agroruta.worker.application.ports.in.NominaUseCase;
import com.agroruta.worker.application.ports.in.TrabajadorUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminService implements AdministrarSistemaUseCase {

    private final UsuarioRepository usuarioRepository;
    private final AdminEstadisticasPort estadisticasPort;
    private final FincaUseCase fincaUseCase;
    private final TrabajadorUseCase trabajadorUseCase;
    private final NominaUseCase nominaUseCase;

    public AdminService(UsuarioRepository usuarioRepository,
                        AdminEstadisticasPort estadisticasPort,
                        FincaUseCase fincaUseCase,
                        TrabajadorUseCase trabajadorUseCase,
                        NominaUseCase nominaUseCase) {
        this.usuarioRepository = usuarioRepository;
        this.estadisticasPort = estadisticasPort;
        this.fincaUseCase = fincaUseCase;
        this.trabajadorUseCase = trabajadorUseCase;
        this.nominaUseCase = nominaUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenSistema resumen() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        long activos = usuarios.stream().filter(Usuario::isActivo).count();
        return new ResumenSistema(
                activos,
                usuarios.size() - activos,
                contarActivos(usuarios, Rol.Administrador),
                contarActivos(usuarios, Rol.Agricultor),
                contarActivos(usuarios, Rol.Trabajador),
                estadisticasPort.estadisticasSistema()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    @Override
    public Usuario cambiarEstado(Long adminId, Long usuarioId, boolean activo) {
        Usuario usuario = buscarUsuario(usuarioId);

        if (!activo) {
            if (Objects.equals(adminId, usuarioId)) {
                throw new BusinessException(ErrorCode.INVALID_OPERATION,
                        "No puedes desactivar tu propia cuenta.");
            }
            exigirOtroAdministrador(usuario);
            usuario.desactivar();
        } else {
            usuario.activar();
        }
        return usuarioRepository.save(usuario);
    }

    @Override
    public Usuario cambiarRol(Long adminId, Long usuarioId, String rolTexto) {
        Rol nuevoRol = parsearRol(rolTexto);
        Usuario usuario = buscarUsuario(usuarioId);

        if (Objects.equals(adminId, usuarioId)) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "No puedes cambiar tu propio rol.");
        }
        if (usuario.getRol() == nuevoRol) {
            return usuario;
        }
        if (nuevoRol != Rol.Administrador) {
            exigirOtroAdministrador(usuario);
        }
        usuario.cambiarRol(nuevoRol);
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgricultorResumen> listarAgricultores() {
        Map<Long, ConteoAgricultor> conteos = estadisticasPort.conteosPorAgricultor();
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() == Rol.Agricultor)
                .map(u -> new AgricultorResumen(u, conteos.getOrDefault(u.getId(), ConteoAgricultor.VACIO)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AgricultorDetalle detalleAgricultor(Long agricultorId) {
        Usuario usuario = buscarUsuario(agricultorId);
        if (usuario.getRol() != Rol.Agricultor) {
            throw new ResourceNotFoundException("Agricultor no encontrado con id: " + agricultorId);
        }
        ConteoAgricultor conteo = estadisticasPort.conteosPorAgricultor()
                .getOrDefault(agricultorId, ConteoAgricultor.VACIO);

        return new AgricultorDetalle(
                new AgricultorResumen(usuario, conteo),
                fincaUseCase.listarFincasPorAgricultor(agricultorId),
                trabajadorUseCase.listarTodosDe(agricultorId),
                nominaUseCase.listarTodasDe(agricultorId)
        );
    }

    // ── Auxiliares ──────────────────────────────────────────────────────────

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    /** Impide dejar el sistema sin administradores activos. */
    private void exigirOtroAdministrador(Usuario usuario) {
        boolean esAdminActivo = usuario.getRol() == Rol.Administrador && usuario.isActivo();
        if (esAdminActivo && usuarioRepository.countActivosPorRol(Rol.Administrador) <= 1) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "Debe quedar al menos un administrador activo.");
        }
    }

    private static long contarActivos(List<Usuario> usuarios, Rol rol) {
        return usuarios.stream().filter(u -> u.isActivo() && u.getRol() == rol).count();
    }

    private static Rol parsearRol(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El rol es obligatorio.");
        }
        return Arrays.stream(Rol.values())
                .filter(r -> r.name().equalsIgnoreCase(texto.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rol no válido: '" + texto + "'. Opciones: Administrador, Agricultor, Trabajador"));
    }
}
