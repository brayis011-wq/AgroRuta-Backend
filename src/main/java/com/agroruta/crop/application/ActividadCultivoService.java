package com.agroruta.crop.application;

import com.agroruta.shared.util.EnumParser;
import com.agroruta.crop.application.ports.in.ActividadCultivoUseCase;
import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.domain.ActividadCultivo;
import com.agroruta.crop.application.ports.out.ActividadCultivoRepository;
import com.agroruta.crop.domain.Siembra;
import com.agroruta.crop.domain.TipoActividad;
import com.agroruta.crop.domain.UsoInsumo;
import com.agroruta.crop.domain.UnidadMedida;
import com.agroruta.crop.domain.Finca;
import com.agroruta.crop.application.ports.out.InventarioPort;
import com.agroruta.crop.application.ports.out.LoteRepository;
import com.agroruta.crop.application.ports.out.FincaRepository;
import java.util.Optional;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import com.agroruta.shared.exception.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ActividadCultivoService implements ActividadCultivoUseCase {

    private final ActividadCultivoRepository actividadRepository;
    private final SiembraUseCase siembraUseCase;

    public ActividadCultivoService(ActividadCultivoRepository actividadRepository,
                                   SiembraUseCase siembraUseCase) {
        this.actividadRepository = actividadRepository;
        this.siembraUseCase = siembraUseCase;
    }

    // ── Bodega de insumos (null en pruebas que no la usan) ──
    private InventarioPort inventario;
    private LoteRepository loteRepository;
    private FincaRepository fincaRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setBodega(InventarioPort inventario, LoteRepository loteRepository, FincaRepository fincaRepository) {
        this.inventario = inventario;
        this.loteRepository = loteRepository;
        this.fincaRepository = fincaRepository;
    }

    /**
     * Deja la bodega al día con la actividad: descuenta la cantidad usada (o corrige el descuento) y, si no
     * se escribió el costo, lo calcula a precio promedio de compra. Sin insumo de la bodega, el producto vuelve.
     */
    private ActividadCultivo sincronizarBodega(ActividadCultivo a) {
        if (inventario == null) return a;
        UsoInsumo uso = a.usoInsumo();
        if (uso == null || !uso.descuentaDeBodega()) {
            inventario.quitarUsoDeActividad(a.getId());
            return a;
        }
        Long agricultorId = agricultorDe(a.getSiembraId());
        if (agricultorId == null) return a;
        Optional<BigDecimal> costo = inventario.registrarUsoDeActividad(a.getId(), agricultorId, uso.insumoId(),
                uso.cantidad(), uso.unidad(), a.getFecha());
        if (a.getCosto() == null && costo.isPresent()) {
            a.setCosto(costo.get());
            return actividadRepository.save(a);
        }
        return a;
    }

    private Long agricultorDe(Long siembraId) {
        if (loteRepository == null || fincaRepository == null) return null;
        Siembra s = siembraUseCase.buscarSiembraPorId(siembraId);
        if (s.getLoteId() == null) return null;
        return loteRepository.findById(s.getLoteId())
                .flatMap(l -> fincaRepository.findById(l.getFincaId()))
                .map(Finca::getAgricultorId).orElse(null);
    }

    private static void validarInsumo(UsoInsumo uso) {
        if (uso == null) return;
        if (uso.cantidad() != null && uso.cantidad() <= 0) throw new BusinessException("La cantidad usada debe ser mayor a 0.");
        if (uso.cantidad() != null) EnumParser.parse(UnidadMedida.class, uso.unidad(), "unidad de la cantidad usada");
    }

    @Override
    public ActividadCultivo registrarActividad(String tipo, String descripcion, LocalDate fecha, Long siembraId,
                                               BigDecimal costo, UsoInsumo insumo) {
        Siembra siembra = siembraUseCase.buscarSiembraPorId(siembraId);
        if (!siembra.estaActiva()) {
            throw new BusinessException("La siembra está finalizada; reábrela para registrar más actividades.");
        }

        if (descripcion == null || descripcion.isBlank())
            throw new BusinessException("La descripción de la actividad es obligatoria.");
        if (fecha == null)
            throw new BusinessException("La fecha de la actividad es obligatoria.");

        TipoActividad tipoActividad = EnumParser.parse(TipoActividad.class, tipo, "tipo de actividad");

        if (actividadRepository.existsByTipoAndFechaAndSiembraId(tipoActividad, fecha, siembraId)) {
            throw repetida(tipo, fecha);
        }

        ActividadCultivo actividad = new ActividadCultivo(
                null, tipoActividad, descripcion, fecha, siembraId
        );
        actividad.setCosto(costo);
        validarInsumo(insumo);
        actividad.usarInsumo(insumo);
        return sincronizarBodega(actividadRepository.save(actividad));
    }

    @Transactional(readOnly = true)
    @Override
    public ActividadCultivo buscarActividadPorId(Long id) {
        return actividadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada con ID: " + id));
    }

    @Override
    public ActividadCultivo actualizarActividad(Long id, String tipo, String descripcion, LocalDate fecha, BigDecimal costo,
                                                UsoInsumo insumo) {
        ActividadCultivo actividad = buscarActividadPorId(id);
        actividad.setCosto(costo);
        validarInsumo(insumo);
        actividad.usarInsumo(insumo);
        return sincronizarBodega(actualizar(actividad, tipo, descripcion, fecha));
    }

    @Override
    public ActividadCultivo actualizarActividad(Long id, String tipo, String descripcion, LocalDate fecha) {
        return actualizar(buscarActividadPorId(id), tipo, descripcion, fecha);
    }

    private ActividadCultivo actualizar(ActividadCultivo actividad, String tipo, String descripcion, LocalDate fecha) {
        if (descripcion == null || descripcion.isBlank())
            throw new BusinessException("La descripción de la actividad es obligatoria.");

        TipoActividad nuevoTipo = tipo != null
                ? EnumParser.parse(TipoActividad.class, tipo, "tipo de actividad")
                : actividad.getTipo();
        LocalDate nuevaFecha = fecha != null ? fecha : actividad.getFecha();
        boolean cambia = nuevoTipo != actividad.getTipo() || !nuevaFecha.equals(actividad.getFecha());
        if (cambia && actividadRepository.existsByTipoAndFechaAndSiembraId(
                nuevoTipo, nuevaFecha, actividad.getSiembraId())) {
            throw repetida(nuevoTipo.name(), nuevaFecha);
        }
        actividad.setTipo(nuevoTipo);
        actividad.setFecha(nuevaFecha);
        actividad.setDescripcion(descripcion.trim());
        return actividadRepository.save(actividad);
    }

    @Override
    public void eliminarActividad(Long id) {
        buscarActividadPorId(id);
        if (inventario != null) inventario.quitarUsoDeActividad(id);
        actividadRepository.deleteById(id);
    }

    private static BusinessException repetida(String tipo, LocalDate fecha) {
        return new BusinessException(
                ErrorCode.RESOURCE_ALREADY_EXISTS,
                String.format("Ya existe una actividad de tipo '%s' registrada el '%s' para esta siembra.", tipo, fecha));
    }

    @Transactional(readOnly = true)
    @Override
    public List<ActividadCultivo> listarActividadesPorSiembra(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        return actividadRepository.findBySiembraId(siembraId);
    }
}