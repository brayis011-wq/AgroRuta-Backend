package com.agroruta.crop.application;

import com.agroruta.shared.util.EnumParser;
import com.agroruta.crop.application.ports.in.FumigacionUseCase;
import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.application.ports.out.InventarioPort;
import com.agroruta.crop.domain.Finca;
import com.agroruta.crop.domain.FincaRepository;
import com.agroruta.crop.domain.Fumigacion;
import com.agroruta.crop.domain.LoteRepository;
import com.agroruta.crop.domain.RestriccionesFumigacion;
import com.agroruta.crop.domain.Siembra;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.crop.domain.FumigacionRepository;
import com.agroruta.crop.domain.UnidadMedida;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Fumigaciones de una siembra. Si el producto es del catálogo y se indica la cantidad total usada,
 * se descuenta de la bodega de insumos del agricultor y, si no se escribió el costo, se calcula
 * con el precio promedio de compra.
 */
@Service
public class FumigacionService implements FumigacionUseCase {

    private final FumigacionRepository fumigacionRepository;
    private final SiembraUseCase siembraUseCase;
    private final InventarioPort inventario;
    private final LoteRepository loteRepository;
    private final FincaRepository fincaRepository;

    public FumigacionService(FumigacionRepository fumigacionRepository, SiembraUseCase siembraUseCase,
                             InventarioPort inventario, LoteRepository loteRepository,
                             FincaRepository fincaRepository) {
        this.fumigacionRepository = fumigacionRepository;
        this.siembraUseCase = siembraUseCase;
        this.inventario = inventario;
        this.loteRepository = loteRepository;
        this.fincaRepository = fincaRepository;
    }

    @Override
    @Transactional
    public Fumigacion registrarFumigacion(LocalDate fecha,
                                          String producto,
                                          Long agriculturalInputId,   // ← nuevo parámetro
                                          Double dosis,
                                          String unidadMedida,
                                          Double areaAplicada,
                                          String observaciones,
                                          Long siembraId,
                                          RestriccionesFumigacion restricciones) {

        Siembra siembra = siembraUseCase.buscarSiembraPorId(siembraId);
        if (!siembra.estaActiva()) {
            throw new BusinessException("La siembra está finalizada; reábrela para registrar más fumigaciones.");
        }

        UnidadMedida unidad = EnumParser.parse(UnidadMedida.class, unidadMedida, "unidad de medida");

        /*
         * Se elige el factory method según si el usuario seleccionó un insumo
         * del catálogo o lo escribió manualmente.
         * En ambos casos `producto` (String) siempre se persiste para
         * mantener el historial aunque el insumo sea eliminado del catálogo.
         */
        Fumigacion fumigacion = (agriculturalInputId != null)
                ? Fumigacion.fromCatalog(fecha, producto, agriculturalInputId,
                dosis, unidad, areaAplicada, observaciones, siembraId)
                : Fumigacion.fromManualEntry(fecha, producto,
                dosis, unidad, areaAplicada, observaciones, siembraId);

        validarCantidad(restricciones);
        fumigacion.fijarRestricciones(restricciones);
        Fumigacion guardada = fumigacionRepository.save(fumigacion);
        return sincronizarBodega(guardada, siembra);
    }

    @Override
    public Fumigacion buscarFumigacionPorId(Long id) {
        return fumigacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fumigación no encontrada con ID: " + id));
    }

    @Override
    @Transactional
    public Fumigacion actualizarFumigacion(Long id, LocalDate fecha, String producto,
                                           Long agriculturalInputId, Double dosis, String unidadMedida,
                                           Double areaAplicada, String observaciones,
                                           RestriccionesFumigacion restricciones) {
        Fumigacion fumigacion = buscarFumigacionPorId(id);
        if (fecha != null) fumigacion.setFecha(fecha);
        if (producto != null && !producto.isBlank()) {
            fumigacion.setProducto(producto.trim());
            // si se escribió a mano, deja de apuntar al catálogo
            fumigacion.setAgriculturalInputId(agriculturalInputId);
        }
        if (dosis != null) fumigacion.setDosis(dosis);
        if (unidadMedida != null) {
            fumigacion.setUnidadMedida(EnumParser.parse(UnidadMedida.class, unidadMedida, "unidad de medida"));
        }
        if (areaAplicada != null) fumigacion.setAreaAplicada(areaAplicada);
        fumigacion.setObservaciones(observaciones);
        // null = no se enviaron (se conservan las guardadas)
        validarCantidad(restricciones);
        if (restricciones != null) fumigacion.fijarRestricciones(restricciones);
        Fumigacion guardada = fumigacionRepository.save(fumigacion);
        return sincronizarBodega(guardada, siembraUseCase.buscarSiembraPorId(guardada.getSiembraId()));
    }

    /** Al borrar la fumigación, su producto vuelve a la bodega */
    @Override
    @Transactional
    public void eliminarFumigacion(Long id) {
        buscarFumigacionPorId(id);
        inventario.quitarUso(id);
        fumigacionRepository.deleteById(id);
    }

    // ── bodega de insumos ─────────────────────────────────────────────

    /**
     * Deja la bodega al día con la fumigación: descuenta la cantidad usada (o corrige el descuento
     * anterior) y, si no se escribió el costo, lo calcula a precio promedio de compra.
     * Si ya no usa un insumo del catálogo o no tiene cantidad, el producto vuelve a la bodega.
     */
    private Fumigacion sincronizarBodega(Fumigacion f, Siembra siembra) {
        if (!f.descuentaDeBodega()) {
            inventario.quitarUso(f.getId());
            return f;
        }
        Long agricultorId = agricultorDe(siembra).orElse(null);
        if (agricultorId == null) return f;
        Optional<Double> costo = inventario.registrarUso(f.getId(), agricultorId, f.getAgriculturalInputId(),
                f.getCantidadUsada(), f.getUnidadCantidad().name(), f.getFecha());
        if (f.getCosto() == null && costo.isPresent()) {
            f.setCosto(costo.get());
            return fumigacionRepository.save(f);
        }
        return f;
    }

    private Optional<Long> agricultorDe(Siembra siembra) {
        if (siembra == null || siembra.getLoteId() == null) return Optional.empty();
        return loteRepository.findById(siembra.getLoteId())
                .flatMap(l -> fincaRepository.findById(l.getFincaId()))
                .map(Finca::getAgricultorId);
    }

    private static void validarCantidad(RestriccionesFumigacion r) {
        if (r != null && r.tieneCantidadUsada()) {
            EnumParser.parse(UnidadMedida.class, r.unidadCantidad(), "unidad de la cantidad usada");
        }
    }

    @Override
    public List<Fumigacion> listarFumigacionesPorSiembra(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        return fumigacionRepository.findBySiembraId(siembraId);
    }
}