package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.in.ActividadProgramadaUseCase;
import com.agroruta.crop.application.ports.out.ActividadProgramadaRepository;
import com.agroruta.crop.application.ports.out.SiembraRepository;
import com.agroruta.crop.domain.ActividadProgramada;
import com.agroruta.crop.domain.AlertaCultivo;
import com.agroruta.crop.domain.EventoCalendario;
import com.agroruta.crop.domain.ProgramacionActividades;
import com.agroruta.crop.domain.Siembra;
import com.agroruta.shared.exception.EstadoInvalidoException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.shared.util.ZonaHoraria;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ActividadProgramadaService implements ActividadProgramadaUseCase {

    static final ZoneId ZONA = ZonaHoraria.COLOMBIA;
    /** Una labor atrasada se recuerda estos días después de la fecha, no todos los días */
    static final Set<Long> DIAS_RECORDAR_ATRASO = Set.of(1L, 3L, 7L, 14L);

    private final ActividadProgramadaRepository repositorio;
    private final SiembraRepository siembras;

    public ActividadProgramadaService(ActividadProgramadaRepository repositorio, SiembraRepository siembras) {
        this.repositorio = repositorio;
        this.siembras = siembras;
    }

    @Override
    public ActividadProgramada crear(
            Long siembraId, String tipo, String descripcion, Integer cadaDias, LocalDate desde, LocalDate hasta) {
        Siembra s = siembras.findById(siembraId).orElseThrow(() -> new ResourceNotFoundException("Siembra", siembraId));
        if (!s.estaActiva())
            throw new EstadoInvalidoException("Esta siembra ya terminó: no se le pueden programar labores.");
        return repositorio.guardar(
                ActividadProgramada.nueva(null, siembraId, tipo, descripcion, cadaDias, desde, hasta));
    }

    @Override
    public ActividadProgramada actualizar(
            Long id, String tipo, String descripcion, Integer cadaDias, LocalDate desde, LocalDate hasta) {
        ActividadProgramada actual =
                repositorio.buscarPorId(id).orElseThrow(() -> new ResourceNotFoundException("Labor programada", id));
        return repositorio.guardar(
                ActividadProgramada.nueva(id, actual.siembraId(), tipo, descripcion, cadaDias, desde, hasta));
    }

    @Override
    public void eliminar(Long id) {
        repositorio.buscarPorId(id).orElseThrow(() -> new ResourceNotFoundException("Labor programada", id));
        repositorio.eliminar(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConEstado> deSiembra(Long siembraId) {
        return conEstado(siembraId, LocalDate.now(ZONA));
    }

    List<ConEstado> conEstado(Long siembraId, LocalDate hoy) {
        List<ActividadProgramada> lista = repositorio.deSiembra(siembraId);
        if (lista.isEmpty()) return List.of();
        Map<String, List<LocalDate>> hechas = repositorio.fechasHechas(siembraId);
        return lista.stream()
                .map(p -> new ConEstado(
                        p, ProgramacionActividades.estado(p, hechas.getOrDefault(p.tipo(), List.of()), hoy)))
                .toList();
    }

    /** Eventos del calendario de una siembra activa entre dos fechas (lo atrasado sale hoy) */
    @Transactional(readOnly = true)
    public List<EventoCalendario> eventos(
            Siembra s, Long loteId, String loteNombre, LocalDate desde, LocalDate hasta, LocalDate hoy) {
        List<EventoCalendario> ev = new ArrayList<>();
        if (s == null || !s.estaActiva()) return ev;
        for (ConEstado c : conEstado(s.getId(), hoy)) {
            ActividadProgramada p = c.programada();
            ProgramacionActividades.Estado e = c.estado();
            if (e.atrasada() && !hoy.isBefore(desde) && !hoy.isAfter(hasta)) {
                ev.add(new EventoCalendario(
                        hoy,
                        "PROGRAMADA",
                        "ATRASADO",
                        p.tipo(),
                        p.descripcion(),
                        s.getId(),
                        loteId,
                        loteNombre,
                        s.nombreCompleto(),
                        p.id()));
            }
            for (LocalDate f : ProgramacionActividades.fechas(p, e, desde, hasta, hoy)) {
                ev.add(new EventoCalendario(
                        f,
                        "PROGRAMADA",
                        "PENDIENTE",
                        p.tipo(),
                        p.descripcion(),
                        s.getId(),
                        loteId,
                        loteNombre,
                        s.nombreCompleto(),
                        p.id()));
            }
        }
        return ev;
    }

    /** Avisos del día: toca hoy, toca mañana o está atrasada (esta última solo algunos días) */
    @Transactional(readOnly = true)
    public List<AlertaCultivo> alertas(Siembra s, String finca, String lote, LocalDate hoy) {
        List<AlertaCultivo> res = new ArrayList<>();
        if (s == null || !s.estaActiva()) return res;
        for (ConEstado c : conEstado(s.getId(), hoy)) {
            ProgramacionActividades.Estado e = c.estado();
            if (e.proxima() == null) continue;
            ActividadProgramada p = c.programada();
            AlertaCultivo.Tipo tipo = null;
            String detalle = p.descripcion();
            if (e.proxima().equals(hoy)) tipo = AlertaCultivo.Tipo.LABOR_HOY;
            else if (e.proxima().equals(hoy.plusDays(1))) tipo = AlertaCultivo.Tipo.LABOR_MANANA;
            else if (e.atrasada() && DIAS_RECORDAR_ATRASO.contains(e.diasAtraso())) {
                tipo = AlertaCultivo.Tipo.LABOR_ATRASADA;
                detalle = String.valueOf(e.diasAtraso());
            }
            if (tipo != null) {
                res.add(new AlertaCultivo(tipo, finca, lote, s.nombreCompleto(), p.tipo(), detalle, e.proxima()));
            }
        }
        return res;
    }
}
