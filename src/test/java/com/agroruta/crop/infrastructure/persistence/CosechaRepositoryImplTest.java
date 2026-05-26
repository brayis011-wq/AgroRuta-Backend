package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.domain.CalidadCosecha;
import com.agroruta.crop.domain.Cosecha;
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
class CosechaRepositoryImplTest {

    @Mock
    private JpaCosechaRepository jpaRepository;

    @InjectMocks
    private CosechaRepositoryImpl repository;

    // ── Fixtures ─────────────────────────────────────────────────

    private static final Long          ID          = 1L;
    private static final Long          SIEMBRA_ID  = 55L;
    private static final LocalDate     FECHA       = LocalDate.of(2024, 9, 20);
    private static final Double        CANTIDAD_KG = 120.5;
    private static final CalidadCosecha CALIDAD    = CalidadCosecha.PRIMERA;
    private static final String        OBSERVACIONES = "Sin novedad";

    private CosechaEntity entityGuardada;
    private Cosecha       dominioEntrada;

    @BeforeEach
    void setUp() {
        entityGuardada = new CosechaEntity();
        entityGuardada.setId(ID);
        entityGuardada.setFecha(FECHA);
        entityGuardada.setCantidadKg(CANTIDAD_KG);
        entityGuardada.setCalidad(CALIDAD);
        entityGuardada.setObservaciones(OBSERVACIONES);
        entityGuardada.setSiembraId(SIEMBRA_ID);

        dominioEntrada = new Cosecha();
        dominioEntrada.setFecha(FECHA);
        dominioEntrada.setCantidadKg(CANTIDAD_KG);
        dominioEntrada.setCalidad(CALIDAD);
        dominioEntrada.setObservaciones(OBSERVACIONES);
        dominioEntrada.setSiembraId(SIEMBRA_ID);
    }

    // ── save ─────────────────────────────────────────────────────

    @Test
    void deberiaSalvarYRetornarCosechaConId() {
        when(jpaRepository.save(any(CosechaEntity.class))).thenReturn(entityGuardada);

        Cosecha resultado = repository.save(dominioEntrada);

        assertEquals(ID,           resultado.getId());
        assertEquals(FECHA,        resultado.getFecha());
        assertEquals(CANTIDAD_KG,  resultado.getCantidadKg());
        assertEquals(CALIDAD,      resultado.getCalidad());
        assertEquals(OBSERVACIONES, resultado.getObservaciones());
        assertEquals(SIEMBRA_ID,   resultado.getSiembraId());
        verify(jpaRepository, times(1)).save(any(CosechaEntity.class));
    }

    @Test
    void saveDeberiaMapearCorrectamenteTodosLosCamposALaEntidad() {
        when(jpaRepository.save(any(CosechaEntity.class))).thenReturn(entityGuardada);

        repository.save(dominioEntrada);

        verify(jpaRepository).save(argThat(entity ->
                entity.getFecha()        .equals(FECHA)         &&
                        entity.getCantidadKg()   .equals(CANTIDAD_KG)   &&
                        entity.getCalidad()      == CALIDAD              &&
                        entity.getObservaciones().equals(OBSERVACIONES)  &&
                        entity.getSiembraId()    .equals(SIEMBRA_ID)
        ));
    }

    @Test
    void saveDeberiaPreservarIdCuandoSeActualizaUnaCosecha() {
        dominioEntrada.setId(ID);
        when(jpaRepository.save(any(CosechaEntity.class))).thenReturn(entityGuardada);

        Cosecha resultado = repository.save(dominioEntrada);

        assertEquals(ID, resultado.getId());
        verify(jpaRepository).save(argThat(entity -> ID.equals(entity.getId())));
    }

    // ── findById ─────────────────────────────────────────────────

    @Test
    void deberiaRetornarCosechaCuandoExisteElId() {
        when(jpaRepository.findById(ID)).thenReturn(Optional.of(entityGuardada));

        Optional<Cosecha> resultado = repository.findById(ID);

        assertTrue(resultado.isPresent());
        assertEquals(ID,            resultado.get().getId());
        assertEquals(FECHA,         resultado.get().getFecha());
        assertEquals(CANTIDAD_KG,   resultado.get().getCantidadKg());
        assertEquals(CALIDAD,       resultado.get().getCalidad());
        assertEquals(OBSERVACIONES, resultado.get().getObservaciones());
        assertEquals(SIEMBRA_ID,    resultado.get().getSiembraId());
    }

    @Test
    void deberiaRetornarVacioSiElIdNoExiste() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Cosecha> resultado = repository.findById(99L);

        assertFalse(resultado.isPresent());
    }

    // ── findBySiembraId ──────────────────────────────────────────

    @Test
    void deberiaRetornarListaDeCosechasPorSiembraId() {
        CosechaEntity entity2 = new CosechaEntity();
        entity2.setId(2L);
        entity2.setFecha(FECHA.plusDays(10));
        entity2.setCantidadKg(80.0);
        entity2.setCalidad(CalidadCosecha.SEGUNDA);
        entity2.setObservaciones("Lote sur");
        entity2.setSiembraId(SIEMBRA_ID);

        when(jpaRepository.findBySiembraId(SIEMBRA_ID))
                .thenReturn(List.of(entityGuardada, entity2));

        List<Cosecha> resultado = repository.findBySiembraId(SIEMBRA_ID);

        assertEquals(2, resultado.size());
        assertEquals(ID,                  resultado.get(0).getId());
        assertEquals(CalidadCosecha.SEGUNDA, resultado.get(1).getCalidad());
    }

    @Test
    void deberiaRetornarListaVaciaSiNoHayCosechasParaLaSiembra() {
        when(jpaRepository.findBySiembraId(SIEMBRA_ID)).thenReturn(List.of());

        List<Cosecha> resultado = repository.findBySiembraId(SIEMBRA_ID);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    // ── existsByFechaAndSiembraId ─────────────────────────────────

    @Test
    void deberiaRetornarTrueSiExisteCosechaConMismaFechaYSiembra() {
        when(jpaRepository.existsByFechaAndSiembraId(FECHA, SIEMBRA_ID)).thenReturn(true);

        assertTrue(repository.existsByFechaAndSiembraId(FECHA, SIEMBRA_ID));
    }

    @Test
    void deberiaRetornarFalseSiNoExisteCosechaConEsaCombinacion() {
        when(jpaRepository.existsByFechaAndSiembraId(FECHA, SIEMBRA_ID)).thenReturn(false);

        assertFalse(repository.existsByFechaAndSiembraId(FECHA, SIEMBRA_ID));
    }

    @Test
    void existsDeberiaDelegarAlJpaConLosParametrosCorrectos() {
        when(jpaRepository.existsByFechaAndSiembraId(any(), any())).thenReturn(false);

        repository.existsByFechaAndSiembraId(FECHA, SIEMBRA_ID);

        verify(jpaRepository, times(1)).existsByFechaAndSiembraId(FECHA, SIEMBRA_ID);
    }

    // ── totalKgBySiembraId ───────────────────────────────────────

    @Test
    void deberiaRetornarTotalKgDeLaSiembra() {
        when(jpaRepository.sumCantidadKgBySiembraId(SIEMBRA_ID)).thenReturn(350.75);

        Double total = repository.totalKgBySiembraId(SIEMBRA_ID);

        assertEquals(350.75, total);
        verify(jpaRepository, times(1)).sumCantidadKgBySiembraId(SIEMBRA_ID);
    }

    @Test
    void deberiaRetornarNullSiNoHayCosechasParaSumarKg() {
        when(jpaRepository.sumCantidadKgBySiembraId(SIEMBRA_ID)).thenReturn(null);

        Double total = repository.totalKgBySiembraId(SIEMBRA_ID);

        assertNull(total);
    }

    // ── deleteById ───────────────────────────────────────────────

    @Test
    void deberiaEliminarCosechaPorId() {
        doNothing().when(jpaRepository).deleteById(ID);

        repository.deleteById(ID);

        verify(jpaRepository, times(1)).deleteById(ID);
    }

    @Test
    void deleteDeberiaLlamarAlJpaConElIdCorrecto() {
        doNothing().when(jpaRepository).deleteById(any());

        repository.deleteById(88L);

        verify(jpaRepository).deleteById(88L);
        verifyNoMoreInteractions(jpaRepository);
    }

    // ── Mapeo toDomain – todas las CalidadCosecha ─────────────────

    @Test
    void deberiaMapearCorrectamenteTodosLosValoresDeCalidad() {
        for (CalidadCosecha calidad : CalidadCosecha.values()) {
            CosechaEntity entity = new CosechaEntity();
            entity.setId(1L);
            entity.setFecha(FECHA);
            entity.setCantidadKg(CANTIDAD_KG);
            entity.setCalidad(calidad);
            entity.setObservaciones(OBSERVACIONES);
            entity.setSiembraId(SIEMBRA_ID);

            when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity));

            Optional<Cosecha> resultado = repository.findById(1L);

            assertTrue(resultado.isPresent());
            assertEquals(calidad, resultado.get().getCalidad());
        }
    }

    // ── Valores límite ───────────────────────────────────────────

    @Test
    void deberiaGuardarCosechaConObservacionesNulas() {
        dominioEntrada.setObservaciones(null);
        entityGuardada.setObservaciones(null);
        when(jpaRepository.save(any(CosechaEntity.class))).thenReturn(entityGuardada);

        Cosecha resultado = repository.save(dominioEntrada);

        assertNull(resultado.getObservaciones());
    }

    @Test
    void deberiaGuardarCosechaConCantidadKgEnCero() {
        dominioEntrada.setCantidadKg(0.0);
        entityGuardada.setCantidadKg(0.0);
        when(jpaRepository.save(any(CosechaEntity.class))).thenReturn(entityGuardada);

        Cosecha resultado = repository.save(dominioEntrada);

        assertEquals(0.0, resultado.getCantidadKg());
    }
}