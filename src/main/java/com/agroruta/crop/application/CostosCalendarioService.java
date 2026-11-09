package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.out.ActividadCultivoRepository;
import com.agroruta.crop.application.ports.out.CosechaRepository;
import com.agroruta.crop.application.ports.out.EtapaSiembraRepository;
import com.agroruta.crop.application.ports.out.FincaRepository;
import com.agroruta.crop.application.ports.out.FumigacionRepository;
import com.agroruta.crop.application.ports.out.LoteRepository;
import com.agroruta.crop.application.ports.out.SiembraRepository;
import com.agroruta.crop.application.ports.in.FincaUseCase;
import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.application.ports.out.ManoObraPort;
import com.agroruta.crop.domain.*;
import com.agroruta.crop.domain.CalendarioCultivo.DatosSiembra;
import com.agroruta.crop.domain.CalendarioCultivo.Historial;
import com.agroruta.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Costos de una temporada y calendario de cultivo (por siembra o por finca). */
@Service
public class CostosCalendarioService {

    static final ZoneId ZONA = ZoneId.of("America/Bogota");
    /** Rango máximo que se puede pedir al calendario */
    static final long MAX_DIAS = 400;

    private final SiembraUseCase siembraUseCase;
    private final FincaUseCase fincaUseCase;
    private final FincaRepository fincaRepository;
    private final LoteRepository loteRepository;
    private final SiembraRepository siembraRepository;
    private final EtapaSiembraRepository etapaRepository;
    private final ActividadCultivoRepository actividadRepository;
    private final FumigacionRepository fumigacionRepository;
    private final CosechaRepository cosechaRepository;
    private final ManoObraPort manoObraPort;

    public CostosCalendarioService(SiembraUseCase siembraUseCase, FincaUseCase fincaUseCase,
                                   FincaRepository fincaRepository, LoteRepository loteRepository,
                                   SiembraRepository siembraRepository, EtapaSiembraRepository etapaRepository,
                                   ActividadCultivoRepository actividadRepository,
                                   FumigacionRepository fumigacionRepository, CosechaRepository cosechaRepository,
                                   ManoObraPort manoObraPort) {
        this.siembraUseCase = siembraUseCase;
        this.fincaUseCase = fincaUseCase;
        this.fincaRepository = fincaRepository;
        this.loteRepository = loteRepository;
        this.siembraRepository = siembraRepository;
        this.etapaRepository = etapaRepository;
        this.actividadRepository = actividadRepository;
        this.fumigacionRepository = fumigacionRepository;
        this.cosechaRepository = cosechaRepository;
        this.manoObraPort = manoObraPort;
    }

    // ── Costos ────────────────────────────────────────────────────────

    public CostosSiembra costos(Long siembraId) {
        siembraUseCase.buscarSiembraPorId(siembraId);
        return CalculadoraCostosSiembra.calcular(siembraId,
                manoObraPort.jornalesDeSiembra(siembraId),
                fumigacionRepository.findBySiembraId(siembraId),
                actividadRepository.findBySiembraId(siembraId),
                cosechaRepository.findBySiembraId(siembraId));
    }

    // ── Calendario ────────────────────────────────────────────────────

    public List<EventoCalendario> calendarioSiembra(Long siembraId, LocalDate desde, LocalDate hasta) {
        Siembra s = siembraUseCase.buscarSiembraPorId(siembraId);
        Lote lote = loteRepository.findById(s.getLoteId()).orElse(null);
        Long agricultorId = lote == null ? null
                : fincaRepository.findById(lote.getFincaId()).map(Finca::getAgricultorId).orElse(null);
        LocalDate[] rango = rango(desde, hasta);
        DatosSiembra datos = datos(s, lote);
        Map<String, Historial> historial = CalendarioCultivo.historialPorCultivo(historialDe(agricultorId));
        return CalendarioCultivo.entre(
                CalendarioCultivo.eventos(datos, historial.get(s.getTipoCultivo()), hoy()), rango[0], rango[1]);
    }

    public List<EventoCalendario> calendarioFinca(Long fincaId, LocalDate desde, LocalDate hasta) {
        Finca finca = fincaUseCase.buscarFincaPorId(fincaId);
        LocalDate[] rango = rango(desde, hasta);
        Map<String, Historial> historial = CalendarioCultivo.historialPorCultivo(historialDe(finca.getAgricultorId()));
        LocalDate hoy = hoy();
        List<EventoCalendario> eventos = new ArrayList<>();
        for (Lote lote : loteRepository.findByFincaId(fincaId)) {
            for (Siembra s : siembraRepository.findHistorialByLoteId(lote.getId())) {
                boolean empezoAntesDelFin = s.getFechaSiembra() == null || !s.getFechaSiembra().isAfter(rango[1]);
                boolean seguiaAlInicio = s.estaActiva() || s.getFechaFin() == null || !s.getFechaFin().isBefore(rango[0]);
                if (!empezoAntesDelFin || !seguiaAlInicio) continue;
                eventos.addAll(CalendarioCultivo.eventos(datos(s, lote), historial.get(s.getTipoCultivo()), hoy));
            }
        }
        return CalendarioCultivo.entre(eventos, rango[0], rango[1]);
    }

    private DatosSiembra datos(Siembra s, Lote lote) {
        Long id = s.getId();
        return new DatosSiembra(s, lote == null ? s.getLoteId() : lote.getId(), lote == null ? null : lote.getNombre(),
                etapaRepository.findBySiembraId(id), actividadRepository.findBySiembraId(id),
                fumigacionRepository.findBySiembraId(id), cosechaRepository.findBySiembraId(id));
    }

    /** Temporadas terminadas del agricultor (todas sus fincas), con etapas y cosechas */
    private List<DatosSiembra> historialDe(Long agricultorId) {
        List<DatosSiembra> lista = new ArrayList<>();
        if (agricultorId == null) return lista;
        for (Finca f : fincaRepository.findByAgricultorId(agricultorId)) {
            for (Lote l : loteRepository.findByFincaId(f.getId())) {
                for (Siembra s : siembraRepository.findHistorialByLoteId(l.getId())) {
                    if (s.estaActiva()) continue;
                    lista.add(new DatosSiembra(s, l.getId(), l.getNombre(), etapaRepository.findBySiembraId(s.getId()),
                            List.of(), List.of(), cosechaRepository.findBySiembraId(s.getId())));
                }
            }
        }
        return lista;
    }

    /** Por defecto: 30 días atrás y 60 adelante */
    private LocalDate[] rango(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = hoy();
        LocalDate d = desde != null ? desde : hoy.minusDays(30);
        LocalDate h = hasta != null ? hasta : hoy.plusDays(60);
        if (h.isBefore(d)) throw new BusinessException("La fecha final no puede ser anterior a la inicial.");
        if (ChronoUnit.DAYS.between(d, h) > MAX_DIAS) throw new BusinessException("El rango del calendario no puede superar 400 días.");
        return new LocalDate[]{d, h};
    }

    LocalDate hoy() {
        return LocalDate.now(ZONA);
    }
}
