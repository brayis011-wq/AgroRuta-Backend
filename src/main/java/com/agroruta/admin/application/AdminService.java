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
import com.agroruta.worker.domain.TrabajadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
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
    private final TrabajadorRepository trabajadorRepository;

    /** Listas ordenadas por nombre, sin importar mayúsculas ni tildes al inicio */
    private static final Comparator<Usuario> POR_NOMBRE = Comparator.comparing(
            (Usuario u) -> u.getNombre() == null ? "" : u.getNombre(), String.CASE_INSENSITIVE_ORDER);

    public AdminService(UsuarioRepository usuarioRepository,
                        AdminEstadisticasPort estadisticasPort,
                        FincaUseCase fincaUseCase,
                        TrabajadorUseCase trabajadorUseCase,
                        NominaUseCase nominaUseCase,
                        TrabajadorRepository trabajadorRepository) {
        this.usuarioRepository = usuarioRepository;
        this.estadisticasPort = estadisticasPort;
        this.fincaUseCase = fincaUseCase;
        this.trabajadorUseCase = trabajadorUseCase;
        this.nominaUseCase = nominaUseCase;
        this.trabajadorRepository = trabajadorRepository;
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
        return usuarioRepository.findAll().stream().sorted(POR_NOMBRE).toList();
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
        exigirSinDatosDelRolActual(usuario);
        usuario.cambiarRol(nuevoRol);
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgricultorResumen> listarAgricultores() {
        Map<Long, ConteoAgricultor> conteos = estadisticasPort.conteosPorAgricultor();
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() == Rol.Agricultor)
                .sorted(POR_NOMBRE)
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

    /**
     * Cambiar el rol no puede dejar datos huérfanos: un agricultor con fincas, trabajadores
     * o nóminas, o un trabajador ya vinculado a una finca, conservan su rol.
     */
    private void exigirSinDatosDelRolActual(Usuario usuario) {
        Long id = usuario.getId();
        if (usuario.getRol() == Rol.Agricultor) {
            boolean tieneDatos = !fincaUseCase.listarFincasPorAgricultor(id).isEmpty()
                    || !trabajadorUseCase.listarTodosDe(id).isEmpty()
                    || !nominaUseCase.listarTodasDe(id).isEmpty();
            if (tieneDatos) {
                throw new BusinessException(ErrorCode.INVALID_OPERATION,
                        "No se puede cambiar el rol: este agricultor ya tiene fincas, trabajadores o nóminas. "
                                + "Si ya no usa la cuenta, desactívala.");
            }
        }
        if (usuario.getRol() == Rol.Trabajador && trabajadorRepository.buscarPorUsuarioId(id).isPresent()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "No se puede cambiar el rol: esta cuenta está vinculada a un trabajador de una finca. "
                            + "Si ya no la usa, desactívala.");
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
