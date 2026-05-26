package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.domain.ActividadCultivo;
import com.agroruta.crop.domain.TipoActividad;
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
class ActividadCultivoRepositoryImplTest {

    @Mock
    private JpaActividadCultivoRepository jpaRepository;

    @InjectMocks
    private ActividadCultivoRepositoryImpl repository;

    // ── Fixtures ─────────────────────────────────────────────────

    private static final Long   ID         = 1L;
    private static final Long   SIEMBRA_ID = 42L;
    private static final LocalDate FECHA   = LocalDate.of(2024, 5, 10);
    private static final TipoActividad TIPO = TipoActividad.RIEGO;
    private static final String DESCRIPCION = "Riego por goteo";

    private ActividadCultivoEntity entityGuardada;
    private ActividadCultivo       dominioEntrada;

    @BeforeEach
    void setUp() {
        entityGuardada = new ActividadCultivoEntity();
        entityGuardada.setId(ID);
        entityGuardada.setTipo(TIPO);
        entityGuardada.setDescripcion(DESCRIPCION);
        entityGuardada.setFecha(FECHA);
        entityGuardada.setSiembraId(SIEMBRA_ID);

        dominioEntrada = new ActividadCultivo();
        dominioEntrada.setTipo(TIPO);
        dominioEntrada.setDescripcion(DESCRIPCION);
        dominioEntrada.setFecha(FECHA);
        dominioEntrada.setSiembraId(SIEMBRA_ID);
    }

    // ── save ─────────────────────────────────────────────────────

    @Test
    void deberiaSalvarYRetornarActividadConId() {
        when(jpaRepository.save(any(ActividadCultivoEntity.class)))
                .thenReturn(entityGuardada);

        ActividadCultivo resultado = repository.save(dominioEntrada);

        assertEquals(ID, resultado.getId());
        assertEquals(TIPO, resultado.getTipo());
        assertEquals(DESCRIPCION, resultado.getDescripcion());
        assertEquals(FECHA, resultado.getFecha());
        assertEquals(SIEMBRA_ID, resultado.getSiembraId());
        verify(jpaRepository, times(1)).save(any(ActividadCultivoEntity.class));
    }

    @Test
    void savDeberiaMapearCorrectamenteTodosLosCamposALaEntidad() {
        when(jpaRepository.save(any(ActividadCultivoEntity.class)))
                .thenReturn(entityGuardada);

        repository.save(dominioEntrada);

        verify(jpaRepository).save(argThat(entity ->
                entity.getTipo()        == TIPO        &&
                        entity.getDescripcion().equals(DESCRIPCION) &&
                        entity.getFecha()       .equals(FECHA) &&
                        entity.getSiembraId()   .equals(SIEMBRA_ID)
        ));
    }

    // ── findById ─────────────────────────────────────────────────

    @Test
    void deberiRetornarActividadCuandoExisteElId() {
        when(jpaRepository.findById(ID)).thenReturn(Optional.of(entityGuardada));

        Optional<ActividadCultivo> resultado = repository.findById(ID);

        assertTrue(resultado.isPresent());
        assertEquals(ID,          resultado.get().getId());
        assertEquals(TIPO,        resultado.get().getTipo());
        assertEquals(DESCRIPCION, resultado.get().getDescripcion());
        assertEquals(FECHA,       resultado.get().getFecha());
        assertEquals(SIEMBRA_ID,  resultado.get().getSiembraId());
    }

    @Test
    void deberiaRetornarVacioSiElIdNoExiste() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<ActividadCultivo> resultado = repository.findById(99L);

        assertFalse(resultado.isPresent());
    }

    // ── findBySiembraId ──────────────────────────────────────────

    @Test
    void deberiaRetornarListaDeActividadesPorSiembraId() {
        ActividadCultivoEntity entity2 = new ActividadCultivoEntity();
        entity2.setId(2L);
        entity2.setTipo(TipoActividad.PODA);
        entity2.setDescripcion("Poda de formación");
        entity2.setFecha(FECHA.plusDays(5));
        entity2.setSiembraId(SIEMBRA_ID);

        when(jpaRepository.findBySiembraId(SIEMBRA_ID))
                .thenReturn(List.of(entityGuardada, entity2));

        List<ActividadCultivo> resultado = repository.findBySiembraId(SIEMBRA_ID);

        assertEquals(2, resultado.size());
        assertEquals(ID,                resultado.get(0).getId());
        assertEquals(TipoActividad.PODA, resultado.get(1).getTipo());
    }

    @Test
    void deberiaRetornarListaVaciaSiNoHayActividadesParaLaSiembra() {
        when(jpaRepository.findBySiembraId(SIEMBRA_ID)).thenReturn(List.of());

        List<ActividadCultivo> resultado = repository.findBySiembraId(SIEMBRA_ID);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    // ── existsByTipoAndFechaAndSiembraId ─────────────────────────

    @Test
    void deberiaRetornarTrueSiExisteActividadConMismoTipoFechaYSiembra() {
        when(jpaRepository.existsByTipoAndFechaAndSiembraId(TIPO, FECHA, SIEMBRA_ID))
                .thenReturn(true);

        assertTrue(repository.existsByTipoAndFechaAndSiembraId(TIPO, FECHA, SIEMBRA_ID));
    }

    @Test
    void deberiaRetornarFalseSiNoExisteActividadConEsasCombinacion() {
        when(jpaRepository.existsByTipoAndFechaAndSiembraId(TIPO, FECHA, SIEMBRA_ID))
                .thenReturn(false);

        assertFalse(repository.existsByTipoAndFechaAndSiembraId(TIPO, FECHA, SIEMBRA_ID));
    }

    @Test
    void existsDeberiaDelegarAlJpaRepositoryConLosParametrosCorrectos() {
        when(jpaRepository.existsByTipoAndFechaAndSiembraId(any(), any(), any()))
                .thenReturn(false);

        repository.existsByTipoAndFechaAndSiembraId(TipoActividad.DESHIERBE, FECHA, SIEMBRA_ID);

        verify(jpaRepository, times(1))
                .existsByTipoAndFechaAndSiembraId(TipoActividad.DESHIERBE, FECHA, SIEMBRA_ID);
    }

    // ── deleteById ───────────────────────────────────────────────

    @Test
    void deberiaEliminarActividadPorId() {
        doNothing().when(jpaRepository).deleteById(ID);

        repository.deleteById(ID);

        verify(jpaRepository, times(1)).deleteById(ID);
    }

    @Test
    void deleteDeberiaLlamarAlJpaConElIdCorrecto() {
        doNothing().when(jpaRepository).deleteById(any());

        repository.deleteById(77L);

        verify(jpaRepository).deleteById(77L);
        verifyNoMoreInteractions(jpaRepository);
    }

    // ── Mapeo toDomain – todos los TipoActividad ─────────────────

    @Test
    void deberiaMapearCorrectamenteTodosLosTiposDeActividad() {
        for (TipoActividad tipo : TipoActividad.values()) {
            ActividadCultivoEntity entity = new ActividadCultivoEntity();
            entity.setId(1L);
            entity.setTipo(tipo);
            entity.setDescripcion("desc");
            entity.setFecha(FECHA);
            entity.setSiembraId(SIEMBRA_ID);

            when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity));

            Optional<ActividadCultivo> resultado = repository.findById(1L);

            assertTrue(resultado.isPresent());
            assertEquals(tipo, resultado.get().getTipo());
        }
    }
}