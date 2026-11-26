package com.agroruta.worker.application;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.DatoInvalidoException;
import com.agroruta.shared.exception.EstadoInvalidoException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.shared.util.Pagina;
import com.agroruta.shared.util.ZonaHoraria;
import com.agroruta.worker.application.ports.in.JornalUseCase;
import com.agroruta.worker.application.ports.out.CultivoPort;
import com.agroruta.worker.application.ports.out.CultivoPort.CultivoInfo;
import com.agroruta.worker.application.ports.out.JornalRepository;
import com.agroruta.worker.application.ports.out.LaborRepository;
import com.agroruta.worker.application.ports.out.NominaRepository;
import com.agroruta.worker.application.ports.out.TrabajadorRepository;
import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.Jornal;
import com.agroruta.worker.domain.Labor;
import com.agroruta.worker.domain.PlanillaSemanal;
import com.agroruta.worker.domain.ResumenJornales;
import com.agroruta.worker.domain.ResumenJornalesInicio;
import com.agroruta.worker.domain.Trabajador;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class JornalService implements JornalUseCase {
    private static final ZoneId ZONA = ZonaHoraria.COLOMBIA;

    private final JornalRepository jornalRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final LaborRepository laborRepository;
    private final CultivoPort cultivoPort;

    public JornalService(
            JornalRepository jornalRepository,
            TrabajadorRepository trabajadorRepository,
            LaborRepository laborRepository,
            CultivoPort cultivoPort) {
        this.jornalRepository = jornalRepository;
        this.trabajadorRepository = trabajadorRepository;
        this.laborRepository = laborRepository;
        this.cultivoPort = cultivoPort;
    }

    public Jornal registrarJornal(
            Long trabajadorId,
            Long cultivoId,
            String nombreCultivo,
            LocalDate fecha,
            List<Long> actividadIds,
            String observaciones) {
        return registrarJornal(trabajadorId, cultivoId, nombreCultivo, fecha, actividadIds, observaciones, null);
    }

    @Override
    public Jornal registrarJornal(
            Long trabajadorId,
            Long cultivoId,
            String nombreCultivo,
            LocalDate fecha,
            List<Long> actividadIds,
            String observaciones,
            java.math.BigDecimal kilos) {
        Jornal jornal = nuevoJornal(trabajadorId, cultivoId, fecha, actividadIds, observaciones, kilos);
        Jornal guardado = jornalRepository.guardar(jornal);
        sumarANominaPendiente(guardado);
        return guardado;
    }

    // ── Nóminas pendientes al día ────────────────────────────────────────────

    /** null en pruebas antiguas: los jornales nuevos no se suman solos a la nómina pendiente */
    private NominaRepository nominaRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setNominaRepository(NominaRepository nominaRepository) {
        this.nominaRepository = nominaRepository;
    }

    /**
     * Si el trabajador ya tiene una nómina pendiente que cubre ese día (se generó la quincena y luego se
     * anotó un día que faltaba), el jornal entra en ella y el total se recalcula.
     */
    private void sumarANominaPendiente(Jornal j) {
        if (nominaRepository == null || j == null || j.getTrabajador() == null) return;
        nominaRepository.buscarPorTrabajadorYEstado(j.getTrabajador().getId(), EstadoNomina.PENDIENTE).stream()
                .filter(n -> n.cubre(j.getFecha()))
                .findFirst()
                .ifPresent(n -> {
                    n.agregarJornales(List.of(j));
                    nominaRepository.guardar(n);
                });
    }

    // ── Planilla semanal ─────────────────────────────────────────────────────

    /** Máximo de días que muestra la planilla (dos semanas, para las quincenas) */
    static final int MAX_DIAS_PLANILLA = 16;

    @Transactional(readOnly = true)
    @Override
    public PlanillaSemanal planilla(Long agricultorId, LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null || fin.isBefore(inicio)) {
            throw new DatoInvalidoException("Indica el primer y el último día de la planilla.");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(inicio, fin) >= MAX_DIAS_PLANILLA) {
            throw new DatoInvalidoException("La planilla muestra como máximo " + MAX_DIAS_PLANILLA + " días.");
        }
        List<Jornal> jornales = jornalRepository.buscarDelEquipo(agricultorId, inicio, fin);
        // Los activos, más quien tuvo jornal esos días aunque ya no esté activo (para no esconder lo anotado)
        java.util.Map<Long, Trabajador> filas = new java.util.LinkedHashMap<>();
        List<Trabajador> activos = agricultorId == null
                ? trabajadorRepository.listarActivos()
                : trabajadorRepository.listarActivosPorAgricultor(agricultorId);
        activos.forEach(t -> filas.put(t.getId(), t));
        jornales.stream()
                .map(Jornal::getTrabajador)
                .filter(Objects::nonNull)
                .forEach(t -> filas.putIfAbsent(t.getId(), t));
        List<Trabajador> ordenados = filas.values().stream()
                .sorted(java.util.Comparator.comparing((Trabajador t) -> !t.estaActivo())
                        .thenComparing(Trabajador::getNombreCompleto, String.CASE_INSENSITIVE_ORDER))
                .toList();
        java.util.Map<Long, EstadoNomina> enNomina = jornalRepository.estadosDeNomina(
                jornales.stream().map(Jornal::getId).toList());
        return new PlanillaSemanal(inicio, fin, ordenados, jornales, enNomina);
    }

    /**
     * Anota varios jornales de una vez (mismo cultivo y labores). Si un trabajador ya tiene jornal ese día
     * en ese mismo cultivo, no se repite. Todo o nada: si uno falla, no queda ninguno.
     */
    @Override
    public List<Jornal> registrarVarios(
            Long agricultorId, Long cultivoId, List<Long> actividadIds, List<PlanillaSemanal.Entrada> entradas) {
        List<Jornal> creados = new java.util.ArrayList<>();
        java.util.Set<String> vistos = new java.util.HashSet<>();
        for (PlanillaSemanal.Entrada e : entradas) {
            Trabajador t = trabajadorRepository
                    .buscarPorId(e.trabajadorId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Trabajador no encontrado con id: " + e.trabajadorId()));
            if (agricultorId != null && !agricultorId.equals(t.getAgricultorId())) {
                throw new BusinessException("Uno de los trabajadores no es de tu equipo.");
            }
            if (!vistos.add(e.trabajadorId() + "|" + e.fecha())) continue;
            boolean yaEsta =
                    jornalRepository.buscarPorTrabajadorYPeriodo(e.trabajadorId(), e.fecha(), e.fecha()).stream()
                            .anyMatch(j -> Objects.equals(j.getCultivoId(), cultivoId));
            if (yaEsta) continue;
            Jornal guardado = jornalRepository.guardar(
                    nuevoJornal(e.trabajadorId(), cultivoId, e.fecha(), actividadIds, null, e.kilos()));
            sumarANominaPendiente(guardado);
            creados.add(guardado);
        }
        return creados;
    }

    /**
     * Quita un jornal anotado por error. Si está en una nómina pendiente, sale de ella y el total se recalcula
     * (si era el único, la nómina pendiente se elimina). No se puede si la nómina ya se aprobó o se pagó.
     */
    @Override
    public void eliminarJornal(Long jornalId) {
        Jornal jornal = jornalRepository
                .buscarPorId(jornalId)
                .orElseThrow(() -> new ResourceNotFoundException("Jornal no encontrado con id: " + jornalId));
        EstadoNomina estado =
                jornalRepository.estadosDeNomina(List.of(jornalId)).get(jornalId);
        if (jornal.isLiquidado() || (estado != null && estado != EstadoNomina.PENDIENTE)) {
            throw new EstadoInvalidoException("Este jornal está en una nómina "
                    + (estado == null ? "pagada" : estado.name().toLowerCase(java.util.Locale.ROOT))
                    + ": para cambiarlo, primero anula esa nómina.");
        }
        if (estado == EstadoNomina.PENDIENTE && nominaRepository != null) {
            nominaRepository.buscarPorJornal(jornalId).ifPresent(n -> {
                if (n.getJornales().size() <= 1) nominaRepository.eliminar(n.getId());
                else {
                    n.quitarJornal(jornalId);
                    nominaRepository.guardar(n);
                }
            });
        }
        jornalRepository.eliminar(jornalId);
    }

    // ── Rendimiento por trabajador ───────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public List<com.agroruta.worker.domain.RendimientoTrabajador> rendimiento(
            Long agricultorId, LocalDate inicio, LocalDate fin) {
        LocalDate hasta = fin != null ? fin : LocalDate.now(ZONA);
        LocalDate desde = inicio != null ? inicio : hasta.minusDays(89);
        if (hasta.isBefore(desde)) throw new DatoInvalidoException("La fecha final no puede ser antes de la inicial.");
        return jornalRepository.rendimiento(agricultorId, desde, hasta);
    }

    // ── Recolectores de una cosecha ──────────────────────────────────────────

    @Override
    public List<Jornal> guardarRecolectores(
            Long cosechaId, Long cultivoId, LocalDate fecha, List<Recoleccion> recolectores) {
        List<Recoleccion> lista = recolectores == null ? List.of() : recolectores;
        java.util.Map<Long, Recoleccion> porTrabajador = new java.util.LinkedHashMap<>();
        for (Recoleccion r : lista) {
            if (r == null || r.trabajadorId() == null)
                throw new DatoInvalidoException("Elige quién recogió en cada fila.");
            if (porTrabajador.put(r.trabajadorId(), r) != null) {
                throw new DatoInvalidoException("Un recolector aparece dos veces en la cosecha.");
            }
        }
        List<Jornal> actuales = jornalRepository.buscarPorCosechas(List.of(cosechaId));
        java.util.Map<Long, EstadoNomina> estados = jornalRepository.estadosDeNomina(
                actuales.stream().map(Jornal::getId).toList());
        java.util.Map<Long, Jornal> actualPorTrabajador = new java.util.HashMap<>();
        actuales.forEach(j -> actualPorTrabajador.put(j.getTrabajador().getId(), j));

        // Lo que ya está en una nómina aprobada o pagada no se toca: debe venir igual
        for (Jornal j : actuales) {
            EstadoNomina estado = estados.get(j.getId());
            if (!fijo(j, estado)) continue;
            Recoleccion r = porTrabajador.get(j.getTrabajador().getId());
            if (r == null || !Objects.equals(fecha, j.getFecha()) || !mismoPago(j, r)) {
                throw new EstadoInvalidoException(
                        "El jornal de " + j.getTrabajador().getNombreCompleto()
                                + " en esta cosecha ya está en una nómina "
                                + (estado == null ? "pagada" : estado.name().toLowerCase(java.util.Locale.ROOT))
                                + ": para cambiarlo, primero anula esa nómina.");
            }
        }

        List<Jornal> resultado = new java.util.ArrayList<>();
        for (Jornal j : actuales) {
            EstadoNomina estado = estados.get(j.getId());
            if (fijo(j, estado)) {
                resultado.add(j);
                continue;
            }
            Recoleccion r = porTrabajador.get(j.getTrabajador().getId());
            if (r == null) eliminarJornal(j.getId());
            else resultado.add(corregirRecolector(j, estado, fecha, r));
        }
        List<Long> labores = laborDeRecoleccion(cultivoId);
        for (Recoleccion r : porTrabajador.values()) {
            if (actualPorTrabajador.containsKey(r.trabajadorId())) continue;
            // Si ya tiene jornal ese día en esta siembra (de la planilla), se le pagaría dos veces
            boolean yaTiene = jornalRepository.buscarPorTrabajadorYPeriodo(r.trabajadorId(), fecha, fecha).stream()
                    .anyMatch(x -> Objects.equals(x.getCultivoId(), cultivoId)
                            && !Objects.equals(x.getCosechaId(), cosechaId));
            Jornal j = nuevoJornalBase(r.trabajadorId(), cultivoId, fecha, labores, "Cosecha");
            if (yaTiene) {
                throw new BusinessException(
                        j.getTrabajador().getNombreCompleto()
                                + " ya tiene jornal ese día en esta siembra: quítalo de la planilla o no lo pongas en la cosecha.");
            }
            j.setCosechaId(cosechaId);
            aplicarPago(j, r);
            Jornal guardado = jornalRepository.guardar(j);
            sumarANominaPendiente(guardado);
            resultado.add(guardado);
        }
        return resultado;
    }

    /** Corrige el jornal de un recolector (sin nómina o en una pendiente) y lo deja en la nómina que le toca */
    private Jornal corregirRecolector(Jornal j, EstadoNomina estado, LocalDate fecha, Recoleccion r) {
        j.setFecha(fecha);
        aplicarPago(j, r);
        Jornal guardado = jornalRepository.guardar(j);
        if (estado == EstadoNomina.PENDIENTE && nominaRepository != null) {
            var nomina = nominaRepository.buscarPorJornal(j.getId());
            if (nomina.isPresent() && nomina.get().cubre(fecha)) {
                nomina.get().reemplazarJornal(guardado);
                nominaRepository.guardar(nomina.get());
                return guardado;
            }
            // La fecha se movió fuera de esa nómina: sale de ella y entra a la que le toque
            nomina.ifPresent(n -> {
                if (n.getJornales().size() <= 1) nominaRepository.eliminar(n.getId());
                else {
                    n.quitarJornal(j.getId());
                    nominaRepository.guardar(n);
                }
            });
        }
        if (estado == null || estado == EstadoNomina.PENDIENTE) sumarANominaPendiente(guardado);
        return guardado;
    }

    private static void aplicarPago(Jornal j, Recoleccion r) {
        if (r.porKilo()) j.pagarPorKilo(r.kilos(), r.valorKilo());
        else {
            j.pagarPorDia(r.valorDia());
            j.anotarKilosRecogidos(r.kilos());
        }
    }

    /** Aprobada, pagada o ya liquidada: no se cambia desde la cosecha */
    private static boolean fijo(Jornal j, EstadoNomina estado) {
        return j.isLiquidado()
                || (estado != null && estado != EstadoNomina.PENDIENTE && estado != EstadoNomina.ANULADA);
    }

    private static boolean mismoPago(Jornal j, Recoleccion r) {
        if (r.porKilo() != (j.getKilos() != null)) return false;
        if (r.porKilo()) {
            return iguales(j.getKilos(), r.kilos())
                    && (r.valorKilo() == null || iguales(j.getValorKilo(), r.valorKilo()));
        }
        return r.valorDia() == null || iguales(j.getValorJornal(), r.valorDia());
    }

    private static boolean iguales(java.math.BigDecimal a, java.math.BigDecimal b) {
        return a != null && b != null && a.compareTo(b) == 0;
    }

    /** La labor "Recolección" (o "Cosecha") del agricultor, si la tiene a la vista */
    private List<Long> laborDeRecoleccion(Long cultivoId) {
        Long agricultorId =
                cultivoPort.buscar(cultivoId).map(CultivoInfo::agricultorId).orElse(null);
        return laborRepository.listarActivasPara(agricultorId).stream()
                .filter(l -> {
                    String n = java.text.Normalizer.normalize(
                                    l.getNombre() == null ? "" : l.getNombre(), java.text.Normalizer.Form.NFD)
                            .replaceAll("\\p{M}", "")
                            .trim()
                            .toLowerCase(java.util.Locale.ROOT);
                    return n.equals("recoleccion") || n.equals("cosecha");
                })
                .sorted(java.util.Comparator.comparing((Labor l) ->
                        !l.getNombre().toLowerCase(java.util.Locale.ROOT).startsWith("recole")))
                .map(Labor::getId)
                .limit(1)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Jornal> jornalesDeCosechas(java.util.Collection<Long> cosechaIds) {
        return jornalRepository.buscarPorCosechas(cosechaIds);
    }

    @Transactional(readOnly = true)
    @Override
    public java.util.Map<Long, EstadoNomina> estadosDeNomina(java.util.Collection<Long> jornalIds) {
        return jornalIds == null || jornalIds.isEmpty()
                ? java.util.Map.of()
                : jornalRepository.estadosDeNomina(jornalIds);
    }

    @Override
    public void quitarRecolectores(Long cosechaId) {
        List<Jornal> actuales = jornalRepository.buscarPorCosechas(List.of(cosechaId));
        java.util.Map<Long, EstadoNomina> estados = jornalRepository.estadosDeNomina(
                actuales.stream().map(Jornal::getId).toList());
        for (Jornal j : actuales) {
            if (fijo(j, estados.get(j.getId()))) {
                // El trabajo se hizo y ya se aprobó o pagó: el jornal queda, solo deja de apuntar a la cosecha
                j.setCosechaId(null);
                jornalRepository.guardar(j);
            } else {
                eliminarJornal(j.getId());
            }
        }
    }

    private Jornal nuevoJornal(
            Long trabajadorId,
            Long cultivoId,
            LocalDate fecha,
            List<Long> actividadIds,
            String observaciones,
            java.math.BigDecimal kilos) {
        Jornal jornal = nuevoJornalBase(trabajadorId, cultivoId, fecha, actividadIds, observaciones);
        jornal.registrarKilos(kilos);
        return jornal;
    }

    /** El jornal con el trabajador, el cultivo y las labores revisados; sin kilos ni valor especial */
    private Jornal nuevoJornalBase(
            Long trabajadorId, Long cultivoId, LocalDate fecha, List<Long> actividadIds, String observaciones) {
        Trabajador trabajador = trabajadorRepository
                .buscarPorId(trabajadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + trabajadorId));

        if (!trabajador.estaActivo()) {
            throw new EstadoInvalidoException("El trabajador " + trabajador.getNombreCompleto() + " no está activo.");
        }

        // El cultivo debe existir, seguir activo y estar en una finca del mismo agricultor.
        // El nombre se toma del cultivo real (lote · finca), no de lo que envíe el cliente.
        CultivoInfo cultivo = cultivoPort
                .buscar(cultivoId)
                .orElseThrow(() -> new ResourceNotFoundException("Cultivo no encontrado con id: " + cultivoId));
        if (!Objects.equals(cultivo.agricultorId(), trabajador.getAgricultorId())) {
            throw new BusinessException("El cultivo no pertenece a tus fincas.");
        }
        if (cultivo.finalizado()) {
            throw new BusinessException("El cultivo de " + cultivo.nombre() + " ya está finalizado.");
        }

        Jornal jornal = new Jornal(null, fecha, trabajador, cultivoId, cultivo.nombre(), observaciones);

        for (Long actividadId : actividadIds) {
            jornal.agregarActividad(actividadUsable(actividadId, trabajador.getAgricultorId()));
        }
        return jornal;
    }

    public Jornal agregarActividad(Long jornalId, Long actividadId) {
        Jornal jornal = jornalRepository
                .buscarPorId(jornalId)
                .orElseThrow(() -> new ResourceNotFoundException("Jornal no encontrado con id: " + jornalId));

        Labor actividad = actividadUsable(actividadId, jornal.getTrabajador().getAgricultorId());

        jornal.agregarActividad(actividad);
        return jornalRepository.guardar(jornal);
    }

    public Jornal removerActividad(Long jornalId, Long actividadId) {
        Jornal jornal = jornalRepository
                .buscarPorId(jornalId)
                .orElseThrow(() -> new ResourceNotFoundException("Jornal no encontrado con id: " + jornalId));

        Labor actividad = laborRepository
                .buscarPorId(actividadId)
                .orElseThrow(() -> new ResourceNotFoundException("Labor no encontrada con id: " + actividadId));

        jornal.removerActividad(actividad);
        return jornalRepository.guardar(jornal);
    }

    /** La labor existe y el agricultor la puede usar (es común o es suya). */
    private Labor actividadUsable(Long actividadId, Long agricultorId) {
        Labor actividad = laborRepository
                .buscarPorId(actividadId)
                .orElseThrow(() -> new ResourceNotFoundException("Labor no encontrada con id: " + actividadId));
        if (!actividad.visiblePara(agricultorId)) {
            throw new BusinessException("La labor " + actividad.getNombre() + " no está disponible en tus fincas.");
        }
        return actividad;
    }

    @Transactional(readOnly = true)
    public Jornal buscarPorId(Long id) {
        return jornalRepository
                .buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Jornal no encontrado con id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Jornal> listarPorTrabajador(Long trabajadorId) {
        return jornalRepository.buscarPorTrabajador(trabajadorId);
    }

    @Transactional(readOnly = true)
    public List<Jornal> listarPorTrabajadorYPeriodo(Long trabajadorId, LocalDate inicio, LocalDate fin) {
        return jornalRepository.buscarPorTrabajadorYPeriodo(trabajadorId, inicio, fin);
    }

    @Transactional(readOnly = true)
    public List<Jornal> listarPorCultivo(Long cultivoId) {
        return jornalRepository.buscarPorCultivo(cultivoId);
    }

    @Transactional(readOnly = true)
    public List<Jornal> listarPorCultivoYPeriodo(Long cultivoId, LocalDate inicio, LocalDate fin) {
        return jornalRepository.buscarPorCultivoYPeriodo(cultivoId, inicio, fin);
    }

    @Transactional(readOnly = true)
    public List<CultivoInfo> cultivosDisponibles(Long agricultorId) {
        return cultivoPort.activosDelAgricultor(agricultorId);
    }

    @Transactional(readOnly = true)
    @Override
    public Pagina<Jornal> paginaPorTrabajador(
            Long trabajadorId, LocalDate inicio, LocalDate fin, int pagina, int tamano) {
        return jornalRepository.paginaPorTrabajador(trabajadorId, inicio, fin, pagina, tamano);
    }

    @Transactional(readOnly = true)
    @Override
    public Pagina<Jornal> paginaDelEquipo(
            Long agricultorId, Long trabajadorId, LocalDate inicio, LocalDate fin, int pagina, int tamano) {
        return jornalRepository.paginaDelEquipo(agricultorId, trabajadorId, inicio, fin, pagina, tamano);
    }

    @Transactional(readOnly = true)
    @Override
    public ResumenJornales resumenDelEquipo(Long agricultorId, Long trabajadorId, LocalDate inicio, LocalDate fin) {
        return jornalRepository.resumenDelEquipo(agricultorId, trabajadorId, inicio, fin);
    }

    @Transactional(readOnly = true)
    @Override
    public ResumenJornales resumenPorTrabajador(Long trabajadorId, LocalDate inicio, LocalDate fin) {
        return jornalRepository.resumenPorTrabajador(trabajadorId, inicio, fin);
    }

    @Transactional(readOnly = true)
    @Override
    public ResumenJornalesInicio resumenInicio(Long agricultorId, LocalDate hoy) {
        return jornalRepository.resumenInicio(agricultorId, hoy != null ? hoy : LocalDate.now(ZONA));
    }
}
