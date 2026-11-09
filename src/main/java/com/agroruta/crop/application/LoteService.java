package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.in.FincaUseCase;
import com.agroruta.crop.application.ports.in.LoteUseCase;
import com.agroruta.crop.domain.EstadoLote;
import com.agroruta.crop.domain.Lote;
import com.agroruta.crop.application.ports.out.LoteRepository;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.shared.util.EnumParser;
import org.springframework.stereotype.Service;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoteService implements LoteUseCase {

    private final LoteRepository loteRepository;
    private final FincaUseCase fincaUseCase;

    public LoteService(LoteRepository loteRepository, FincaUseCase fincaUseCase) {
        this.loteRepository = loteRepository;
        this.fincaUseCase = fincaUseCase;
    }

    @Override
    public Lote registrarLote(String nombre, Double area, Long fincaId) {

        fincaUseCase.buscarFincaPorId(fincaId);

        if (loteRepository.existsByNombreAndFincaId(nombre, fincaId)) {
            throw nombreRepetido(nombre);
        }

        Lote lote = new Lote(null, nombre, area, fincaId);
        return loteRepository.save(lote);
    }

    @Override
    public Lote actualizarGeometriaLote(Long loteId, String coordenadas,
                                        Double area, Double centroideLat,
                                        Double centroideLng) {
        Lote lote = buscarLotePorId(loteId);
        lote.actualizarGeometria(coordenadas, area, centroideLat, centroideLng);
        return loteRepository.save(lote);
    }

    @Override
    public Lote actualizarLote(Long id, String nombre, Double area) {
        Lote lote = buscarLotePorId(id);
        boolean cambiaNombre = nombre != null && !nombre.trim().equalsIgnoreCase(lote.getNombre());
        if (cambiaNombre && loteRepository.existsByNombreAndFincaId(nombre.trim(), lote.getFincaId())) {
            throw nombreRepetido(nombre.trim());
        }
        lote.editar(nombre, area);
        return loteRepository.save(lote);
    }

    @Override
    public Lote cambiarEstado(Long id, String estado) {
        Lote lote = buscarLotePorId(id);
        EstadoLote nuevo = EnumParser.parse(EstadoLote.class, estado, "estado del lote");
        if (nuevo == EstadoLote.EN_CULTIVO) {
            throw new BusinessException("El lote pasa a 'En cultivo' solo al registrar una siembra.");
        }
        if (loteRepository.existsSiembraActivaEnLote(id)) {
            throw new BusinessException("El lote tiene una siembra en curso. Finalízala antes de cambiar su estado.");
        }
        lote.setEstado(nuevo);
        return loteRepository.save(lote);
    }

    @Override
    public void ocuparLote(Long id) {
        Lote lote = buscarLotePorId(id);
        if (lote.getEstado() != EstadoLote.EN_CULTIVO) {
            lote.iniciarCultivo();
            loteRepository.save(lote);
        }
    }

    @Override
    public void liberarLote(Long id, EstadoLote estado) {
        Lote lote = buscarLotePorId(id);
        lote.setEstado(estado == EstadoLote.EN_DESCANSO ? EstadoLote.EN_DESCANSO : EstadoLote.DISPONIBLE);
        loteRepository.save(lote);
    }

    @Override
    public Lote buscarLotePorId(Long id) {
        return loteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + id));
    }

    @Override
    public List<Lote> listarLotesPorFinca(Long fincaId) {
        fincaUseCase.buscarFincaPorId(fincaId);
        return loteRepository.findByFincaId(fincaId);
    }

    @Override
    /**
     * Borrado lógico: el lote deja de verse, pero sus siembras (con sus gastos e ingresos)
     * siguen en los reportes. No se puede si tiene una siembra en curso.
     */
    public void eliminarLote(Long id) {
        Lote lote = buscarLotePorId(id);
        if (lote.estaEliminado()) return;
        if (loteRepository.existsSiembraActivaEnLote(id)) {
            throw new BusinessException("El lote tiene una siembra en curso. Finalízala o elimínala antes de borrar el lote.");
        }
        lote.eliminar(LocalDateTime.now());
        loteRepository.save(lote);
    }

    private static BusinessException nombreRepetido(String nombre) {
        return new BusinessException(
                ErrorCode.RESOURCE_ALREADY_EXISTS,
                String.format("Ya existe un lote con el nombre '%s' en esta finca.", nombre));
    }
}
