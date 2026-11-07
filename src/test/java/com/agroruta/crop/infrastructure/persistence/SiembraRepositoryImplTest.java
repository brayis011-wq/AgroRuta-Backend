package com.agroruta.crop.infrastructure.persistence;

import java.time.LocalDateTime;
import com.agroruta.crop.domain.EstadoCultivo;
import com.agroruta.crop.domain.Siembra;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiembraRepositoryImplTest {

    @Mock
    private JpaSiembraRepository jpaRepository;

    @InjectMocks
    private SiembraRepositoryImpl repository;

    // ── Fixtures ─────────────────────────────────────────────────

    private static final Long          ID               = 1L;
    private static final Long          LOTE_ID          = 15L;
    private static final LocalDate     FECHA_SIEMBRA    = LocalDate.of(2024, 3, 1);
    private static final Integer       CANTIDAD_PLANTAS = 200;
    private static final String        VARIEDAD         = "Colombia";
    private static final EstadoCultivo  ESTADO          = EstadoCultivo.CRECIMIENTO;

    private SiembraEntity entityGuardada;
    private Siembra       dominioEntrada;

    @BeforeEach
    void setUp() {
        entityGuardada = new SiembraEntity();
        entityGuardada.setId(ID);
        entityGuardada.setFechaSiembra(FECHA_SIEMBRA);
        entityGuardada.setCantidadPlantas(CANTIDAD_PLANTAS);
        entityGuardada.setVariedad(VARIEDAD);
        entityGuardada.setEstadoCultivo(ESTADO);
        entityGuardada.setLoteId(LOTE_ID);

        dominioEntrada = new Siembra();
        dominioEntrada.setFechaSiembra(FECHA_SIEMBRA);
        dominioEntrada.setCantidadPlantas(CANTIDAD_PLANTAS);
        dominioEntrada.setVariedad(VARIEDAD);
        dominioEntrada.setEstadoCultivo(ESTADO);
        dominioEntrada.setLoteId(LOTE_ID);
    }

    // ── save ─────────────────────────────────────────────────────

    @Test
    void deberiaSalvarYRetornarSiembraConId() {
        when(jpaRepository.save(any(SiembraEntity.class))).thenReturn(entityGuardada);

        Siembra resultado = repository.save(dominioEntrada);

        assertEquals(ID,               resultado.getId());
        assertEquals(FECHA_SIEMBRA,    resultado.getFechaSiembra());
        assertEquals(CANTIDAD_PLANTAS, resultado.getCantidadPlantas());
        assertEquals(VARIEDAD,         resultado.getVariedad());
        assertEquals(ESTADO,           resultado.getEstadoCultivo());
        assertEquals(LOTE_ID,          resultado.getLoteId());
        verify(jpaRepository, times(1)).save(any(SiembraEntity.class));
    }

    @Test
    void saveDeberiaMapearCorrectamenteTodosLosCamposALaEntidad() {
        when(jpaRepository.save(any(SiembraEntity.class))).thenReturn(entityGuardada);

        repository.save(dominioEntrada);

        verify(jpaRepository).save(argThat(entity ->
                entity.getFechaSiembra()   .equals(FECHA_SIEMBRA)   &&
                        entity.getCantidadPlantas().equals(CANTIDAD_PLANTAS) &&
                        VARIEDAD.equals(entity.getVariedad())              &&
                        entity.getEstadoCultivo()  == ESTADO                 &&
                        entity.getLoteId()         .equals(LOTE_ID)
        ));
    }

    @Test
    void saveDeberiaPreservarIdCuandoSeActualizaUnaSiembra() {
        dominioEntrada.setId(ID);
        when(jpaRepository.save(any(SiembraEntity.class))).thenReturn(entityGuardada);

        Siembra resultado = repository.save(dominioEntrada);

        assertEquals(ID, resultado.getId());
        verify(jpaRepository).save(argThat(entity -> ID.equals(entity.getId())));
    }

    // ── findById ─────────────────────────────────────────────────

    @Test
    void deberiaRetornarSiembraCuandoExisteElId() {
        when(jpaRepository.findById(ID)).thenReturn(Optional.of(entityGuardada));

        Optional<Siembra> resultado = repository.findById(ID);

        assertTrue(resultado.isPresent());
        assertEquals(ID,               resultado.get().getId());
        assertEquals(FECHA_SIEMBRA,    resultado.get().getFechaSiembra());
        assertEquals(CANTIDAD_PLANTAS, resultado.get().getCantidadPlantas());
        assertEquals(VARIEDAD,         resultado.get().getVariedad());
        assertEquals(ESTADO,           resultado.get().getEstadoCultivo());
        assertEquals(LOTE_ID,          resultado.get().getLoteId());
    }

    @Test
    void deberiaRetornarVacioSiElIdNoExiste() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Siembra> resultado = repository.findById(99L);

        assertFalse(resultado.isPresent());
    }

    // ── findActivaByLoteId / historial ───────────────────────────

    @Test
    void deberiaRetornarLaSiembraEnCursoDelLote() {
        when(jpaRepository.findFirstByLoteIdAndEstadoCultivoNotAndEliminadoEnIsNullOrderByFechaSiembraDescIdDesc(
                LOTE_ID, EstadoCultivo.FINALIZADO)).thenReturn(Optional.of(entityGuardada));

        Optional<Siembra> resultado = repository.findActivaByLoteId(LOTE_ID);

        assertTrue(resultado.isPresent());
        assertEquals(ID,      resultado.get().getId());
        assertEquals(LOTE_ID, resultado.get().getLoteId());
        assertEquals(ESTADO,  resultado.get().getEstadoCultivo());
    }

    @Test
    void deberiaRetornarVacioSiNoHaySiembraEnCursoEnElLote() {
        when(jpaRepository.findFirstByLoteIdAndEstadoCultivoNotAndEliminadoEnIsNullOrderByFechaSiembraDescIdDesc(
                LOTE_ID, EstadoCultivo.FINALIZADO)).thenReturn(Optional.empty());

        assertFalse(repository.findActivaByLoteId(LOTE_ID).isPresent());
    }

    @Test
    void deberiaRetornarTodasLasTemporadasDelLoteConSuFechaFin() {
        SiembraEntity anterior = new SiembraEntity();
        anterior.setId(2L);
        anterior.setFechaSiembra(FECHA_SIEMBRA.minusYears(1));
        anterior.setCantidadPlantas(100);
        anterior.setVariedad("Gigante");
        anterior.setEstadoCultivo(EstadoCultivo.FINALIZADO);
        anterior.setLoteId(LOTE_ID);
        anterior.setFechaFin(FECHA_SIEMBRA.minusDays(20));
        when(jpaRepository.findByLoteIdAndEliminadoEnIsNullOrderByFechaSiembraDescIdDesc(LOTE_ID))
                .thenReturn(List.of(entityGuardada, anterior));

        List<Siembra> resultado = repository.findHistorialByLoteId(LOTE_ID);

        assertEquals(2, resultado.size());
        assertEquals(ID, resultado.get(0).getId());
        assertEquals(FECHA_SIEMBRA.minusDays(20), resultado.get(1).getFechaFin());
    }

    // ── findByEstadoCultivo ───────────────────────────────────────

    @Test
    void deberiaRetornarListaDeSiembrasPorEstado() {
        SiembraEntity entity2 = new SiembraEntity();
        entity2.setId(2L);
        entity2.setFechaSiembra(FECHA_SIEMBRA.plusDays(10));
        entity2.setCantidadPlantas(150);
        entity2.setVariedad("Gigante");
        entity2.setEstadoCultivo(ESTADO);
        entity2.setLoteId(16L);

        when(jpaRepository.findByEstadoCultivoAndEliminadoEnIsNull(ESTADO))
                .thenReturn(List.of(entityGuardada, entity2));

        List<Siembra> resultado = repository.findByEstadoCultivo(ESTADO);

        assertEquals(2,                    resultado.size());
        assertEquals(ID,                   resultado.get(0).getId());
        assertEquals("Gigante", resultado.get(1).getVariedad());
        assertEquals(ESTADO,               resultado.get(1).getEstadoCultivo());
    }

    @Test
    void deberiaRetornarListaVaciaSiNoHaySiembraConEseEstado() {
        when(jpaRepository.findByEstadoCultivoAndEliminadoEnIsNull(EstadoCultivo.FINALIZADO))
                .thenReturn(List.of());

        List<Siembra> resultado = repository.findByEstadoCultivo(EstadoCultivo.FINALIZADO);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void findByEstadoDeberiaDelegarAlJpaConElEstadoCorrecto() {
        when(jpaRepository.findByEstadoCultivoAndEliminadoEnIsNull(any())).thenReturn(List.of());

        repository.findByEstadoCultivo(EstadoCultivo.PRODUCCION);

        verify(jpaRepository, times(1)).findByEstadoCultivoAndEliminadoEnIsNull(EstadoCultivo.PRODUCCION);
    }

    // ── existsActivaByLoteId ──────────────────────────────────────

    @Test
    void deberiaRetornarTrueSiHaySiembraEnCursoEnElLote() {
        when(jpaRepository.existsByLoteIdAndEstadoCultivoNotAndEliminadoEnIsNull(LOTE_ID, EstadoCultivo.FINALIZADO)).thenReturn(true);

        assertTrue(repository.existsActivaByLoteId(LOTE_ID));
    }

    @Test
    void deberiaRetornarFalseSiSoloHaySiembrasFinalizadas() {
        when(jpaRepository.existsByLoteIdAndEstadoCultivoNotAndEliminadoEnIsNull(LOTE_ID, EstadoCultivo.FINALIZADO)).thenReturn(false);

        assertFalse(repository.existsActivaByLoteId(LOTE_ID));
    }

    // ── Mapeo toDomain – todos los EstadoCultivo ──────────────────

    @Test
    void deberiaMapearCorrectamenteTodosLosEstadosDeCultivo() {
        for (EstadoCultivo estado : EstadoCultivo.values()) {
            SiembraEntity entity = new SiembraEntity();
            entity.setId(ID);
            entity.setFechaSiembra(FECHA_SIEMBRA);
            entity.setCantidadPlantas(CANTIDAD_PLANTAS);
            entity.setVariedad(VARIEDAD);
            entity.setEstadoCultivo(estado);
            entity.setLoteId(LOTE_ID);

            when(jpaRepository.findById(ID)).thenReturn(Optional.of(entity));

            Optional<Siembra> resultado = repository.findById(ID);

            assertTrue(resultado.isPresent());
            assertEquals(estado, resultado.get().getEstadoCultivo());
        }
    }

    // ── Mapeo toDomain – todas las variedades ─────────────────

    @Test
    void deberiaMapearCorrectamenteTodosLosValoresDeVariedad() {
        for (String variedad : List.of("Colombia", "Gigante", "Kenya")) {
            SiembraEntity entity = new SiembraEntity();
            entity.setId(ID);
            entity.setFechaSiembra(FECHA_SIEMBRA);
            entity.setCantidadPlantas(CANTIDAD_PLANTAS);
            entity.setVariedad(variedad);
            entity.setEstadoCultivo(ESTADO);
            entity.setLoteId(LOTE_ID);

            when(jpaRepository.findById(ID)).thenReturn(Optional.of(entity));

            Optional<Siembra> resultado = repository.findById(ID);

            assertTrue(resultado.isPresent());
            assertEquals(variedad, resultado.get().getVariedad());
        }
    }

    @Test
    void findByEstadoDeberiaCubrirTodosLosEstadosEnListado() {
        for (EstadoCultivo estado : EstadoCultivo.values()) {
            SiembraEntity entity = new SiembraEntity();
            entity.setId(ID);
            entity.setFechaSiembra(FECHA_SIEMBRA);
            entity.setCantidadPlantas(CANTIDAD_PLANTAS);
            entity.setVariedad(VARIEDAD);
            entity.setEstadoCultivo(estado);
            entity.setLoteId(LOTE_ID);

            when(jpaRepository.findByEstadoCultivoAndEliminadoEnIsNull(estado))
                    .thenReturn(List.of(entity));

            List<Siembra> resultado = repository.findByEstadoCultivo(estado);

            assertEquals(1, resultado.size());
            assertEquals(estado, resultado.get(0).getEstadoCultivo());
        }
    }
    // ── Borrado lógico ───────────────────────────────────────────

    @Test
    void saveConservaLaFechaDeEliminacion() {
        LocalDateTime cuando = LocalDateTime.of(2026, 11, 7, 10, 30);
        Siembra siembra = new Siembra();
        siembra.setFechaSiembra(java.time.LocalDate.of(2026, 9, 1));
        siembra.setCantidadPlantas(100);
        siembra.setVariedad("Castillo");
        siembra.setEstadoCultivo(EstadoCultivo.FINALIZADO);
        siembra.setLoteId(3L);
        siembra.eliminar(java.time.LocalDate.of(2026, 11, 7), cuando);
        when(jpaRepository.save(any(SiembraEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Siembra guardada = repository.save(siembra);

        assertEquals(cuando, guardada.getEliminadoEn());
        assertTrue(guardada.estaEliminada());
    }
}
