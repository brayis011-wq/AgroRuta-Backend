package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.domain.Fumigacion;
import com.agroruta.crop.domain.UnidadMedida;
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
class FumigacionRepositoryImplTest {

    @Mock
    private JpaFumigacionRepository jpaRepository;

    @InjectMocks
    private FumigacionRepositoryImpl repository;

    // ── Fixtures ─────────────────────────────────────────────────

    private static final Long        ID            = 1L;
    private static final Long        SIEMBRA_ID    = 33L;
    private static final LocalDate   FECHA         = LocalDate.of(2024, 7, 10);
    private static final String      PRODUCTO      = "Glifosato";
    private static final Double      DOSIS         = 2.5;
    private static final UnidadMedida UNIDAD       = UnidadMedida.LITROS;
    private static final Double      AREA_APLICADA = 8.0;
    private static final String      OBSERVACIONES = "Aplicación zona norte";

    private FumigacionEntity entityGuardada;
    private Fumigacion       dominioEntrada;

    @BeforeEach
    void setUp() {
        entityGuardada = new FumigacionEntity();
        entityGuardada.setId(ID);
        entityGuardada.setFecha(FECHA);
        entityGuardada.setProducto(PRODUCTO);
        entityGuardada.setDosis(DOSIS);
        entityGuardada.setUnidadMedida(UNIDAD);
        entityGuardada.setAreaAplicada(AREA_APLICADA);
        entityGuardada.setObservaciones(OBSERVACIONES);
        entityGuardada.setSiembraId(SIEMBRA_ID);

        dominioEntrada = new Fumigacion();
        dominioEntrada.setFecha(FECHA);
        dominioEntrada.setProducto(PRODUCTO);
        dominioEntrada.setDosis(DOSIS);
        dominioEntrada.setUnidadMedida(UNIDAD);
        dominioEntrada.setAreaAplicada(AREA_APLICADA);
        dominioEntrada.setObservaciones(OBSERVACIONES);
        dominioEntrada.setSiembraId(SIEMBRA_ID);
    }

    // ── save ─────────────────────────────────────────────────────

    @Test
    void deberiaSalvarYRetornarFumigacionConId() {
        when(jpaRepository.save(any(FumigacionEntity.class))).thenReturn(entityGuardada);

        Fumigacion resultado = repository.save(dominioEntrada);

        assertEquals(ID,            resultado.getId());
        assertEquals(FECHA,         resultado.getFecha());
        assertEquals(PRODUCTO,      resultado.getProducto());
        assertEquals(DOSIS,         resultado.getDosis());
        assertEquals(UNIDAD,        resultado.getUnidadMedida());
        assertEquals(AREA_APLICADA, resultado.getAreaAplicada());
        assertEquals(OBSERVACIONES, resultado.getObservaciones());
        assertEquals(SIEMBRA_ID,    resultado.getSiembraId());
        verify(jpaRepository, times(1)).save(any(FumigacionEntity.class));
    }

    @Test
    void saveDeberiaMapearCorrectamenteTodosLosCamposALaEntidad() {
        when(jpaRepository.save(any(FumigacionEntity.class))).thenReturn(entityGuardada);

        repository.save(dominioEntrada);

        verify(jpaRepository).save(argThat(entity ->
                entity.getFecha()        .equals(FECHA)         &&
                        entity.getProducto()     .equals(PRODUCTO)      &&
                        entity.getDosis()        .equals(DOSIS)         &&
                        entity.getUnidadMedida() == UNIDAD              &&
                        entity.getAreaAplicada() .equals(AREA_APLICADA) &&
                        entity.getObservaciones().equals(OBSERVACIONES) &&
                        entity.getSiembraId()    .equals(SIEMBRA_ID)
        ));
    }

    @Test
    void saveDeberiaPreservarIdCuandoSeActualizaUnaFumigacion() {
        dominioEntrada.setId(ID);
        when(jpaRepository.save(any(FumigacionEntity.class))).thenReturn(entityGuardada);

        Fumigacion resultado = repository.save(dominioEntrada);

        assertEquals(ID, resultado.getId());
        verify(jpaRepository).save(argThat(entity -> ID.equals(entity.getId())));
    }

    @Test
    void deberiaGuardarFumigacionConObservacionesNulas() {
        dominioEntrada.setObservaciones(null);
        entityGuardada.setObservaciones(null);
        when(jpaRepository.save(any(FumigacionEntity.class))).thenReturn(entityGuardada);

        Fumigacion resultado = repository.save(dominioEntrada);

        assertNull(resultado.getObservaciones());
    }

    // ── findById ─────────────────────────────────────────────────

    @Test
    void deberiaRetornarFumigacionCuandoExisteElId() {
        when(jpaRepository.findById(ID)).thenReturn(Optional.of(entityGuardada));

        Optional<Fumigacion> resultado = repository.findById(ID);

        assertTrue(resultado.isPresent());
        assertEquals(ID,            resultado.get().getId());
        assertEquals(FECHA,         resultado.get().getFecha());
        assertEquals(PRODUCTO,      resultado.get().getProducto());
        assertEquals(DOSIS,         resultado.get().getDosis());
        assertEquals(UNIDAD,        resultado.get().getUnidadMedida());
        assertEquals(AREA_APLICADA, resultado.get().getAreaAplicada());
        assertEquals(OBSERVACIONES, resultado.get().getObservaciones());
        assertEquals(SIEMBRA_ID,    resultado.get().getSiembraId());
    }

    @Test
    void deberiaRetornarVacioSiElIdNoExiste() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Fumigacion> resultado = repository.findById(99L);

        assertFalse(resultado.isPresent());
    }

    // ── findBySiembraId ──────────────────────────────────────────

    @Test
    void deberiaRetornarListaDeFumigacionesPorSiembraId() {
        FumigacionEntity entity2 = new FumigacionEntity();
        entity2.setId(2L);
        entity2.setFecha(FECHA.plusDays(7));
        entity2.setProducto("Cobre");
        entity2.setDosis(1.0);
        entity2.setUnidadMedida(UnidadMedida.KG);
        entity2.setAreaAplicada(4.0);
        entity2.setObservaciones("Segunda aplicación");
        entity2.setSiembraId(SIEMBRA_ID);

        when(jpaRepository.findBySiembraId(SIEMBRA_ID))
                .thenReturn(List.of(entityGuardada, entity2));

        List<Fumigacion> resultado = repository.findBySiembraId(SIEMBRA_ID);

        assertEquals(2,              resultado.size());
        assertEquals(ID,             resultado.get(0).getId());
        assertEquals(UnidadMedida.KG, resultado.get(1).getUnidadMedida());
    }

    @Test
    void deberiaRetornarListaVaciaSiNoHayFumigacionesParaLaSiembra() {
        when(jpaRepository.findBySiembraId(SIEMBRA_ID)).thenReturn(List.of());

        List<Fumigacion> resultado = repository.findBySiembraId(SIEMBRA_ID);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void findBySiembraIdDeberiaMapearTodasLasFumigacionesCorrectamente() {
        FumigacionEntity entity2 = new FumigacionEntity();
        entity2.setId(2L);
        entity2.setFecha(FECHA.plusDays(3));
        entity2.setProducto("Azufre");
        entity2.setDosis(0.5);
        entity2.setUnidadMedida(UnidadMedida.GRAMOS);
        entity2.setAreaAplicada(2.0);
        entity2.setObservaciones(null);
        entity2.setSiembraId(SIEMBRA_ID);

        when(jpaRepository.findBySiembraId(SIEMBRA_ID))
                .thenReturn(List.of(entityGuardada, entity2));

        List<Fumigacion> resultado = repository.findBySiembraId(SIEMBRA_ID);

        assertEquals(SIEMBRA_ID,          resultado.get(0).getSiembraId());
        assertEquals(SIEMBRA_ID,          resultado.get(1).getSiembraId());
        assertEquals(UnidadMedida.GRAMOS, resultado.get(1).getUnidadMedida());
        assertNull(resultado.get(1).getObservaciones());
    }

    // ── deleteById ───────────────────────────────────────────────

    @Test
    void deberiaEliminarFumigacionPorId() {
        doNothing().when(jpaRepository).deleteById(ID);

        repository.deleteById(ID);

        verify(jpaRepository, times(1)).deleteById(ID);
    }

    @Test
    void deleteDeberiaLlamarAlJpaConElIdCorrecto() {
        doNothing().when(jpaRepository).deleteById(any());

        repository.deleteById(55L);

        verify(jpaRepository).deleteById(55L);
        verifyNoMoreInteractions(jpaRepository);
    }

    // ── Mapeo toDomain – todas las UnidadMedida ──────────────────

    @Test
    void deberiaMapearCorrectamenteTodosLosValoresDeUnidadMedida() {
        for (UnidadMedida unidad : UnidadMedida.values()) {
            FumigacionEntity entity = new FumigacionEntity();
            entity.setId(ID);
            entity.setFecha(FECHA);
            entity.setProducto(PRODUCTO);
            entity.setDosis(DOSIS);
            entity.setUnidadMedida(unidad);
            entity.setAreaAplicada(AREA_APLICADA);
            entity.setObservaciones(OBSERVACIONES);
            entity.setSiembraId(SIEMBRA_ID);

            when(jpaRepository.findById(ID)).thenReturn(Optional.of(entity));

            Optional<Fumigacion> resultado = repository.findById(ID);

            assertTrue(resultado.isPresent());
            assertEquals(unidad, resultado.get().getUnidadMedida());
        }
    }

    // ── Valores límite ───────────────────────────────────────────

    @Test
    void deberiaGuardarFumigacionConDosisYAreaEnCero() {
        dominioEntrada.setDosis(0.0);
        dominioEntrada.setAreaAplicada(0.0);
        entityGuardada.setDosis(0.0);
        entityGuardada.setAreaAplicada(0.0);
        when(jpaRepository.save(any(FumigacionEntity.class))).thenReturn(entityGuardada);

        Fumigacion resultado = repository.save(dominioEntrada);

        assertEquals(0.0, resultado.getDosis());
        assertEquals(0.0, resultado.getAreaAplicada());
    }
}