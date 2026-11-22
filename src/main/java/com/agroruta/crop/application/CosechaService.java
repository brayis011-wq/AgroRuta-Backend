package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.out.CosechaRepository;
import com.agroruta.crop.application.ports.out.FumigacionRepository;
import com.agroruta.crop.application.ports.out.RecolectoresPort;
import com.agroruta.shared.util.EnumParser;
import com.agroruta.crop.application.ports.in.CosechaUseCase;
import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.domain.*;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CosechaService implements CosechaUseCase {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-CO"));

    private final CosechaRepository cosechaRepository;
    private final SiembraUseCase siembraUseCase;
    private final FumigacionRepository fumigacionRepository;

    public CosechaService(CosechaRepository cosechaRepository,
                          SiembraUseCase siembraUseCase,
                          FumigacionRepository fumigacionRepository) {
        this.cosechaRepository = cosechaRepository;
        this.siembraUseCase = siembraUseCase;
        this.fumigacionRepository = fumigacionRepository;
    }

    // ── Recolectores (jornales del equipo); null en pruebas que no los usan ──
    private RecolectoresPort recolectoresPort;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setRecolectoresPort(RecolectoresPort recolectoresPort) {
        this.recolectoresPort = recolectoresPort;
    }

    @Override
    public Cosecha registrarCosecha(LocalDate fecha, Double cantidadKg, String calidad, String observaciones,
                                    Long siembraId, BigDecimal precioKg, boolean confirmarCarencia,
                                    List<Recolector> recolectores) {

        Siembra siembra = siembraUseCase.buscarSiembraPorId(siembraId);
        if (!siembra.estaActiva()) {
            throw new BusinessException("La siembra está finalizada; reábrela para registrar más cosechas.");
        }

        // El mismo día se puede cosechar primera, segunda y tercera; lo que no se repite es la calidad
        CalidadCosecha calidadCosecha = EnumParser.parse(CalidadCosecha.class, calidad, "calidad");
        if (cosechaRepository.existsByFechaAndSiembraIdAndCalidad(fecha, siembraId, calidadCosecha)) {
            throw repetida(fecha, calidadCosecha);
        }

        Cosecha cosecha = new Cosecha(null, fecha, cantidadKg, calidadCosecha, observaciones, siembraId);
        cosecha.setPrecioKg(precioKg);
        cosecha.setEnCarencia(revisarCarencia(siembraId, fecha, confirmarCarencia));
        revisarKilos(cantidadKg, recolectores);
        Cosecha guardada = cosechaRepository.save(cosecha);
        if (recolectores != null && !recolectores.isEmpty() && recolectoresPort != null) {
            recolectoresPort.guardar(guardada.getId(), siembraId, guardada.getFecha(), recolectores);
        }
        return conRecolectores(guardada);
    }

    /** Los kilos de los recolectores pagados por kilo no pueden pasar de lo cosechado */
    private static void revisarKilos(Double cantidadKg, List<Recolector> recolectores) {
        if (recolectores == null || cantidadKg == null) return;
        BigDecimal suma = recolectores.stream()
                .filter(r -> r != null && r.porKilo() && r.kilos() != null)
                .map(Recolector::kilos).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (suma.compareTo(BigDecimal.valueOf(cantidadKg).add(new BigDecimal("0.01"))) > 0) {
            throw new BusinessException(String.format(Locale.ROOT,
                    "Los recolectores suman %s kg, más de lo cosechado (%s kg).",
                    suma.stripTrailingZeros().toPlainString(), BigDecimal.valueOf(cantidadKg).stripTrailingZeros().toPlainString()));
        }
    }

    private Cosecha conRecolectores(Cosecha c) {
        if (recolectoresPort != null && c != null && c.getId() != null) {
            c.setRecolectores(recolectoresPort.deCosechas(List.of(c.getId())).getOrDefault(c.getId(), List.of()));
        }
        return c;
    }

    private List<Cosecha> conRecolectores(List<Cosecha> lista) {
        if (recolectoresPort == null || lista.isEmpty()) return lista;
        var mapa = recolectoresPort.deCosechas(lista.stream().map(Cosecha::getId).toList());
        lista.forEach(c -> c.setRecolectores(mapa.getOrDefault(c.getId(), List.of())));
        return lista;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Cosecha> listarCosechasPorSiembra(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        return conRecolectores(cosechaRepository.findBySiembraId(siembraId));
    }

    @Transactional(readOnly = true)
    @Override
    public Cosecha buscarCosechaPorId(Long id) {
        return conRecolectores(cosechaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cosecha no encontrada con ID: " + id)));
    }

    @Override
    public Cosecha actualizarCosecha(Long id, LocalDate fecha, Double cantidadKg,
                                     String calidad, String observaciones, BigDecimal precioKg, boolean confirmarCarencia,
                                     List<Recolector> recolectores) {
        Cosecha cosecha = buscarCosechaPorId(id);
        cosecha.setPrecioKg(precioKg);
        LocalDate antes = cosecha.getFecha();
        List<Recolector> lista = recolectores;
        if (lista == null && fecha != null && !fecha.equals(antes) && !cosecha.getRecolectores().isEmpty()) {
            // Sin lista nueva pero con otra fecha: los mismos recolectores pasan a la fecha nueva
            lista = cosecha.getRecolectores().stream().map(r -> new Recolector(r.trabajadorId(), r.modalidad(), r.kilos(),
                    r.valorKilo(), "DIA".equals(r.modalidad()) ? r.valor() : null)).toList();
        }
        revisarKilos(cantidadKg != null ? cantidadKg : cosecha.getCantidadKg(), lista);
        Cosecha guardada = aplicarCambios(cosecha, fecha, cantidadKg, calidad, observaciones, confirmarCarencia);
        if (lista != null && recolectoresPort != null) {
            recolectoresPort.guardar(guardada.getId(), guardada.getSiembraId(), guardada.getFecha(), lista);
        }
        return conRecolectores(guardada);
    }

    @Override
    public Cosecha actualizarCosecha(Long id, LocalDate fecha, Double cantidadKg,
                                     String calidad, String observaciones) {
        return aplicarCambios(buscarCosechaPorId(id), fecha, cantidadKg, calidad, observaciones, false);
    }

    private Cosecha aplicarCambios(Cosecha cosecha, LocalDate fecha, Double cantidadKg,
                                   String calidad, String observaciones, boolean confirmarCarencia) {
        boolean cambiaFecha = fecha != null && !fecha.equals(cosecha.getFecha());
        CalidadCosecha calidadFinal = calidad != null
                ? EnumParser.parse(CalidadCosecha.class, calidad, "calidad") : cosecha.getCalidad();
        if (cambiaFecha || calidadFinal != cosecha.getCalidad()) {
            LocalDate dia = cambiaFecha ? fecha : cosecha.getFecha();
            if (cosechaRepository.existsByFechaAndSiembraIdAndCalidad(dia, cosecha.getSiembraId(), calidadFinal)) {
                throw repetida(dia, calidadFinal);
            }
        }
        // Si solo se corrigen los kilos o el precio de una cosecha que ya estaba confirmada, no se vuelve a preguntar
        if (cambiaFecha || !cosecha.isEnCarencia()) {
            LocalDate fechaFinal = fecha != null ? fecha : cosecha.getFecha();
            cosecha.setEnCarencia(revisarCarencia(cosecha.getSiembraId(), fechaFinal, confirmarCarencia));
        }
        if (fecha != null) cosecha.setFecha(fecha);
        if (cantidadKg != null) cosecha.setCantidadKg(cantidadKg);
        cosecha.setCalidad(calidadFinal);
        cosecha.setObservaciones(observaciones);
        return cosechaRepository.save(cosecha);
    }

    /**
     * ¿La fecha cae en la carencia de una fumigación de la siembra? Sin confirmación se rechaza;
     * con confirmación devuelve true para que la cosecha quede marcada.
     */
    private boolean revisarCarencia(Long siembraId, LocalDate fecha, boolean confirmada) {
        var bloqueo = Carencia.queBloquea(fumigacionRepository.findBySiembraId(siembraId), fecha);
        if (bloqueo.isEmpty()) return false;
        if (confirmada) return true;
        Carencia.Bloqueo b = bloqueo.get();
        throw new BusinessException(ErrorCode.HARVEST_IN_WITHDRAWAL, String.format(
                "Todavía no se puede cosechar: %s se aplicó el %s y tiene %d días de carencia. Se puede cosechar desde el %s.",
                b.producto() == null || b.producto().isBlank() ? "un producto" : b.producto(),
                b.aplicado().format(FECHA), b.dias(), b.libreDesde().format(FECHA)));
    }

    @Override
    public void eliminarCosecha(Long id) {
        buscarCosechaPorId(id);
        if (recolectoresPort != null) recolectoresPort.quitar(id);
        cosechaRepository.deleteById(id);
    }

    private static BusinessException repetida(LocalDate fecha, CalidadCosecha calidad) {
        return new BusinessException(
                ErrorCode.RESOURCE_ALREADY_EXISTS,
                String.format("Ya existe una cosecha de %s calidad registrada el '%s' para esta siembra.",
                        calidad.name().toLowerCase(Locale.ROOT), fecha));
    }

    @Transactional(readOnly = true)
    @Override
    public Double totalKgCosechado(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        Double total = cosechaRepository.totalKgBySiembraId(siembraId);
        return total != null ? total : 0.0;
    }
}
