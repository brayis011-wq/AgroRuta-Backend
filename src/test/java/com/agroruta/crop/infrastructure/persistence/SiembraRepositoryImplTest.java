package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.domain.EstadoCultivo;
import com.agroruta.crop.domain.Siembra;
import com.agroruta.crop.domain.VariedadUchuva;
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
    private static final VariedadUchuva VARIEDAD        = VariedadUchuva.COLOMBIA;
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
                        entity.getVariedad()       == VARIEDAD               &&
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

    // ── findByLoteId ─────────────────────────────────────────────

    @Test
    void deberiaRetornarSiembraCuandoExisteElLoteId() {
        when(jpaRepository.findByLoteId(LOTE_ID)).thenReturn(Optional.of(entityGuardada));

        Optional<Siembra> resultado = repository.findByLoteId(LOTE_ID);

        assertTrue(resultado.isPresent());
        assertEquals(ID,      resultado.get().getId());
        assertEquals(LOTE_ID, resultado.get().getLoteId());
        assertEquals(ESTADO,  resultado.get().getEstadoCultivo());
    }

    @Test
    void deberiaRetornarVacioSiNoHaySiembraEnElLote() {
        when(jpaRepository.findByLoteId(LOTE_ID)).thenReturn(Optional.empty());

        Optional<Siembra> resultado = repository.findByLoteId(LOTE_ID);

        assertFalse(resultado.isPresent());
    }

    @Test
    void findByLoteIdDeberiaDelegarAlJpaConElParametroCorrecto() {
        when(jpaRepository.findByLoteId(LOTE_ID)).thenReturn(Optional.empty());

        repository.findByLoteId(LOTE_ID);

        verify(jpaRepository, times(1)).findByLoteId(LOTE_ID);
    }

    // ── findByEstadoCultivo ───────────────────────────────────────

    @Test
    void deberiaRetornarListaDeSiembrasPorEstado() {
        SiembraEntity entity2 = new SiembraEntity();
        entity2.setId(2L);
        entity2.setFechaSiembra(FECHA_SIEMBRA.plusDays(10));
        entity2.setCantidadPlantas(150);
        entity2.setVariedad(VariedadUchuva.GIGANTE);
        entity2.setEstadoCultivo(ESTADO);
        entity2.setLoteId(16L);

        when(jpaRepository.findByEstadoCultivo(ESTADO))
                .thenReturn(List.of(entityGuardada, entity2));

        List<Siembra> resultado = repository.findByEstadoCultivo(ESTADO);

        assertEquals(2,                    resultado.size());
        assertEquals(ID,                   resultado.get(0).getId());
        assertEquals(VariedadUchuva.GIGANTE, resultado.get(1).getVariedad());
        assertEquals(ESTADO,               resultado.get(1).getEstadoCultivo());
    }

    @Test
    void deberiaRetornarListaVaciaSiNoHaySiembraConEseEstado() {
        when(jpaRepository.findByEstadoCultivo(EstadoCultivo.FINALIZADO))
                .thenReturn(List.of());

        List<Siembra> resultado = repository.findByEstadoCultivo(EstadoCultivo.FINALIZADO);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void findByEstadoDeberiaDelegarAlJpaConElEstadoCorrecto() {
        when(jpaRepository.findByEstadoCultivo(any())).thenReturn(List.of());

        repository.findByEstadoCultivo(EstadoCultivo.PRODUCCION);

        verify(jpaRepository, times(1)).findByEstadoCultivo(EstadoCultivo.PRODUCCION);
    }

    // ── existsByLoteId ────────────────────────────────────────────

    @Test
    void deberiaRetornarTrueSiExisteSiembraEnElLote() {
        when(jpaRepository.existsByLoteId(LOTE_ID)).thenReturn(true);

        assertTrue(repository.existsByLoteId(LOTE_ID));
    }

    @Test
    void deberiaRetornarFalseSiNoExisteSiembraEnElLote() {
        when(jpaRepository.existsByLoteId(LOTE_ID)).thenReturn(false);

        assertFalse(repository.existsByLoteId(LOTE_ID));
    }

    @Test
    void existsByLoteIdDeberiaDelegarAlJpaConElParametroCorrecto() {
        when(jpaRepository.existsByLoteId(any())).thenReturn(false);

        repository.existsByLoteId(LOTE_ID);

        verify(jpaRepository, times(1)).existsByLoteId(LOTE_ID);
    }

    // ── deleteById ───────────────────────────────────────────────

    @Test
    void deberiaEliminarSiembraPorId() {
        doNothing().when(jpaRepository).deleteById(ID);

        repository.deleteById(ID);

        verify(jpaRepository, times(1)).deleteById(ID);
    }

    @Test
    void deleteDeberiaLlamarAlJpaConElIdCorrecto() {
        doNothing().when(jpaRepository).deleteById(any());

        repository.deleteById(44L);

        verify(jpaRepository).deleteById(44L);
        verifyNoMoreInteractions(jpaRepository);
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

    // ── Mapeo toDomain – todas las VariedadUchuva ─────────────────

    @Test
    void deberiaMapearCorrectamenteTodosLosValoresDeVariedad() {
        for (VariedadUchuva variedad : VariedadUchuva.values()) {
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

            when(jpaRepository.findByEstadoCultivo(estado))
                    .thenReturn(List.of(entity));

            List<Siembra> resultado = repository.findByEstadoCultivo(estado);

            assertEquals(1, resultado.size());
            assertEquals(estado, resultado.get(0).getEstadoCultivo());
        }
    }
}