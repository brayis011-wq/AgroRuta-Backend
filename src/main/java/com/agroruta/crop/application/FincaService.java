package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.in.FincaUseCase;
import com.agroruta.crop.domain.Finca;
import com.agroruta.crop.application.ports.out.FincaRepository;
import com.agroruta.crop.domain.Lote;
import com.agroruta.crop.application.ports.out.LoteRepository;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FincaService implements FincaUseCase {

    private final FincaRepository fincaRepository;
    // Repositorio (no LoteUseCase) para no crear un ciclo: LoteService ya depende de FincaUseCase
    private final LoteRepository loteRepository;

    public FincaService(FincaRepository fincaRepository, LoteRepository loteRepository) {
        this.fincaRepository = fincaRepository;
        this.loteRepository = loteRepository;
    }

    @Override
    public Finca registrarFinca(String nombre, String ubicacion,
                                Double hectareas, Long agricultorId) {


        if (fincaRepository.existsByNombreAndAgricultorId(nombre, agricultorId)) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    String.format("Ya tienes una finca registrada con el nombre '%s'", nombre)
            );
        }

        Finca finca = new Finca(null, nombre, ubicacion, hectareas, agricultorId);
        return fincaRepository.save(finca);
    }

    @Override
    public Finca buscarFincaPorId(Long id) {
        return fincaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Finca no encontrada con ID: " + id));
    }

    @Override
    public List<Finca> listarFincasPorAgricultor(Long agricultorId) {
        return fincaRepository.findByAgricultorId(agricultorId);
    }

    /**
     * Borrado lógico de la finca y de sus lotes: dejan de verse, pero sus siembras (con sus
     * gastos e ingresos) siguen en los reportes. No se puede si algún lote tiene una siembra en curso.
     */
    @Override
    @Transactional
    public void eliminarFinca(Long id) {
        Finca finca = buscarFincaPorId(id);
        if (finca.estaEliminada()) return;
        List<Lote> lotes = loteRepository.findByFincaId(id);
        for (Lote lote : lotes) {
            if (loteRepository.existsSiembraActivaEnLote(lote.getId())) {
                throw new BusinessException("El lote '" + lote.getNombre() + "' tiene una siembra en curso. "
                        + "Finalízala o elimínala antes de borrar la finca.");
            }
        }
        LocalDateTime ahora = LocalDateTime.now();
        for (Lote lote : lotes) {
            lote.eliminar(ahora);
            loteRepository.save(lote);
        }
        finca.eliminar(ahora);
        fincaRepository.save(finca);
    }
    @Override
    public Finca actualizarFinca(Long id, String nombre, String ubicacion, Double hectareas) {
        Finca finca = buscarFincaPorId(id);
        boolean cambiaNombre = nombre != null && !nombre.trim().equalsIgnoreCase(finca.getNombre());
        if (cambiaNombre && fincaRepository.existsByNombreAndAgricultorId(nombre.trim(), finca.getAgricultorId())) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    String.format("Ya tienes una finca registrada con el nombre '%s'", nombre.trim()));
        }
        finca.editar(nombre, ubicacion, hectareas);
        return fincaRepository.save(finca);
    }

    @Override
    public Finca actualizarCentroideFinca(Long fincaId, Double lat, Double lng) {
        Finca finca = buscarFincaPorId(fincaId);
        finca.actualizarCentroide(lat, lng);
        return fincaRepository.save(finca);
    }
}