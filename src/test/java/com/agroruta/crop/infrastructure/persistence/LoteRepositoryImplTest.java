package com.agroruta.crop.infrastructure.persistence;

import java.time.LocalDateTime;
import com.agroruta.crop.domain.EstadoCultivo;
import com.agroruta.crop.domain.EstadoLote;
import com.agroruta.crop.domain.Lote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoteRepositoryImplTest {

    @Mock
    private JpaLoteRepository jpaRepository;

    @Mock
    private JpaSiembraRepository jpaSiembraRepository;

    @InjectMocks
    private LoteRepositoryImpl repository;

    // ── Fixtures ─────────────────────────────────────────────────

    private static final Long      ID        = 1L;
    private static final Long      FINCA_ID  = 20L;
    private static final String    NOMBRE    = "Lote Norte";
    private static final Double    AREA      = 12.5;
    private static final EstadoLote ESTADO   = EstadoLote.DISPONIBLE;
    private static final String    COORDENADAS = "[[4.71,-74.07],[4.72,-74.08]]";
    private static final Double    LAT       = 4.7110;
    private static final Double    LNG       = -74.0721;

    private LoteEntity entityGuardada;
    private Lote       dominioEntrada;

    @BeforeEach
    void setUp() {
        entityGuardada = new LoteEntity();
        entityGuardada.setId(ID);
        entityGuardada.setNombre(NOMBRE);
        entityGuardada.setArea(AREA);
        entityGuardada.setEstado(ESTADO);
        entityGuardada.setFincaId(FINCA_ID);
        entityGuardada.setCoordenadas(COORDENADAS);
        entityGuardada.setCentroideLat(LAT);
        entityGuardada.setCentroideLng(LNG);

        dominioEntrada = new Lote();
        dominioEntrada.setNombre(NOMBRE);
        dominioEntrada.setArea(AREA);
        dominioEntrada.setEstado(ESTADO);
        dominioEntrada.setFincaId(FINCA_ID);
        dominioEntrada.setCoordenadas(COORDENADAS);
        dominioEntrada.setCentroideLat(LAT);
        dominioEntrada.setCentroideLng(LNG);
    }

    // ── save ─────────────────────────────────────────────────────

    @Test
    void deberiaSalvarYRetornarLoteConId() {
        when(jpaRepository.save(any(LoteEntity.class))).thenReturn(entityGuardada);

        Lote resultado = repository.save(dominioEntrada);

        assertEquals(ID,          resultado.getId());
        assertEquals(NOMBRE,      resultado.getNombre());
        assertEquals(AREA,        resultado.getArea());
        assertEquals(ESTADO,      resultado.getEstado());
        assertEquals(FINCA_ID,    resultado.getFincaId());
        assertEquals(COORDENADAS, resultado.getCoordenadas());
        assertEquals(LAT,         resultado.getCentroideLat());
        assertEquals(LNG,         resultado.getCentroideLng());
        verify(jpaRepository, times(1)).save(any(LoteEntity.class));
    }

    @Test
    void saveDeberiaMapearCorrectamenteTodosLosCamposALaEntidad() {
        when(jpaRepository.save(any(LoteEntity.class))).thenReturn(entityGuardada);

        repository.save(dominioEntrada);

        verify(jpaRepository).save(argThat(entity ->
                entity.getNombre()      .equals(NOMBRE)       &&
                        entity.getArea()        .equals(AREA)         &&
                        entity.getEstado()      == ESTADO             &&
                        entity.getFincaId()     .equals(FINCA_ID)     &&
                        entity.getCoordenadas() .equals(COORDENADAS)  &&
                        entity.getCentroideLat().equals(LAT)          &&
                        entity.getCentroideLng().equals(LNG)
        ));
    }

    @Test
    void saveDeberiaPreservarIdCuandoSeActualizaUnLote() {
        dominioEntrada.setId(ID);
        when(jpaRepository.save(any(LoteEntity.class))).thenReturn(entityGuardada);

        Lote resultado = repository.save(dominioEntrada);

        assertEquals(ID, resultado.getId());
        verify(jpaRepository).save(argThat(entity -> ID.equals(entity.getId())));
    }

    @Test
    void deberiaGuardarLoteSinCentroideNiCoordenadas() {
        dominioEntrada.setCentroideLat(null);
        dominioEntrada.setCentroideLng(null);
        dominioEntrada.setCoordenadas(null);
        entityGuardada.setCentroideLat(null);
        entityGuardada.setCentroideLng(null);
        entityGuardada.setCoordenadas(null);
        when(jpaRepository.save(any(LoteEntity.class))).thenReturn(entityGuardada);

        Lote resultado = repository.save(dominioEntrada);

        assertNull(resultado.getCentroideLat());
        assertNull(resultado.getCentroideLng());
        assertNull(resultado.getCoordenadas());
    }

    // ── findById ─────────────────────────────────────────────────

    @Test
    void deberiaRetornarLoteCuandoExisteElId() {
        when(jpaRepository.findById(ID)).thenReturn(Optional.of(entityGuardada));

        Optional<Lote> resultado = repository.findById(ID);

        assertTrue(resultado.isPresent());
        assertEquals(ID,          resultado.get().getId());
        assertEquals(NOMBRE,      resultado.get().getNombre());
        assertEquals(AREA,        resultado.get().getArea());
        assertEquals(ESTADO,      resultado.get().getEstado());
        assertEquals(FINCA_ID,    resultado.get().getFincaId());
        assertEquals(COORDENADAS, resultado.get().getCoordenadas());
        assertEquals(LAT,         resultado.get().getCentroideLat());
        assertEquals(LNG,         resultado.get().getCentroideLng());
    }

    @Test
    void deberiaRetornarVacioSiElIdNoExiste() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Lote> resultado = repository.findById(99L);

        assertFalse(resultado.isPresent());
    }

    @Test
    void findByIdDeberiaMapearCamposOpcionalesNulosCorrectamente() {
        entityGuardada.setCentroideLat(null);
        entityGuardada.setCentroideLng(null);
        entityGuardada.setCoordenadas(null);
        when(jpaRepository.findById(ID)).thenReturn(Optional.of(entityGuardada));

        Optional<Lote> resultado = repository.findById(ID);

        assertTrue(resultado.isPresent());
        assertNull(resultado.get().getCentroideLat());
        assertNull(resultado.get().getCentroideLng());
        assertNull(resultado.get().getCoordenadas());
    }

    // ── findByFincaId ─────────────────────────────────────────────

    @Test
    void deberiaRetornarListaDeLotesPorFincaId() {
        LoteEntity entity2 = new LoteEntity();
        entity2.setId(2L);
        entity2.setNombre("Lote Sur");
        entity2.setArea(7.0);
        entity2.setEstado(EstadoLote.EN_CULTIVO);
        entity2.setFincaId(FINCA_ID);
        entity2.setCoordenadas(null);

        when(jpaRepository.findByFincaIdAndEliminadoEnIsNull(FINCA_ID))
                .thenReturn(List.of(entityGuardada, entity2));

        List<Lote> resultado = repository.findByFincaId(FINCA_ID);

        assertEquals(2,                   resultado.size());
        assertEquals(NOMBRE,              resultado.get(0).getNombre());
        assertEquals(EstadoLote.EN_CULTIVO, resultado.get(1).getEstado());
    }

    @Test
    void deberiaRetornarListaVaciaSiLaFincaNoTieneLotes() {
        when(jpaRepository.findByFincaIdAndEliminadoEnIsNull(FINCA_ID)).thenReturn(List.of());

        List<Lote> resultado = repository.findByFincaId(FINCA_ID);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void findByFincaIdDeberiaMapearTodosLosLotesCorrectamente() {
        LoteEntity entity2 = new LoteEntity();
        entity2.setId(2L);
        entity2.setNombre("Lote Este");
        entity2.setArea(5.0);
        entity2.setEstado(EstadoLote.EN_DESCANSO);
        entity2.setFincaId(FINCA_ID);
        entity2.setCentroideLat(5.0);
        entity2.setCentroideLng(-75.0);

        when(jpaRepository.findByFincaIdAndEliminadoEnIsNull(FINCA_ID))
                .thenReturn(List.of(entityGuardada, entity2));

        List<Lote> resultado = repository.findByFincaId(FINCA_ID);

        assertEquals(FINCA_ID,              resultado.get(0).getFincaId());
        assertEquals(FINCA_ID,              resultado.get(1).getFincaId());
        assertEquals(EstadoLote.EN_DESCANSO, resultado.get(1).getEstado());
        assertEquals(5.0,                   resultado.get(1).getCentroideLat());
    }

    // ── existsSiembraActivaEnLote ─────────────────────────────────

    @Test
    void deberiaRetornarTrueSiExisteSiembraActivaEnElLote() {
        when(jpaSiembraRepository.existsByLoteIdAndEstadoCultivoNotAndEliminadoEnIsNull(ID, EstadoCultivo.FINALIZADO))
                .thenReturn(true);

        assertTrue(repository.existsSiembraActivaEnLote(ID));
    }

    @Test
    void deberiaRetornarFalseSiNoExisteSiembraActivaEnElLote() {
        when(jpaSiembraRepository.existsByLoteIdAndEstadoCultivoNotAndEliminadoEnIsNull(ID, EstadoCultivo.FINALIZADO))
                .thenReturn(false);

        assertFalse(repository.existsSiembraActivaEnLote(ID));
    }

    @Test
    void existsSiembraActivaDeberiaDelegarAlJpaSiembraConElLoteIdCorrecto() {
        when(jpaSiembraRepository.existsByLoteIdAndEstadoCultivoNotAndEliminadoEnIsNull(ID, EstadoCultivo.FINALIZADO))
                .thenReturn(false);

        repository.existsSiembraActivaEnLote(ID);

        verify(jpaSiembraRepository, times(1)).existsByLoteIdAndEstadoCultivoNotAndEliminadoEnIsNull(ID, EstadoCultivo.FINALIZADO);
        verifyNoInteractions(jpaRepository);
    }

    // ── existsByNombreAndFincaId ──────────────────────────────────

    @Test
    void deberiaRetornarTrueSiExisteLoteConMismoNombreYFinca() {
        when(jpaRepository.existsByNombreAndFincaIdAndEliminadoEnIsNull(NOMBRE, FINCA_ID)).thenReturn(true);

        assertTrue(repository.existsByNombreAndFincaId(NOMBRE, FINCA_ID));
    }

    @Test
    void deberiaRetornarFalseSiNoExisteLoteConEsaCombinacion() {
        when(jpaRepository.existsByNombreAndFincaIdAndEliminadoEnIsNull(NOMBRE, FINCA_ID)).thenReturn(false);

        assertFalse(repository.existsByNombreAndFincaId(NOMBRE, FINCA_ID));
    }

    @Test
    void existsByNombreDeberiaDelegarAlJpaConLosParametrosCorrectos() {
        when(jpaRepository.existsByNombreAndFincaIdAndEliminadoEnIsNull(any(), any())).thenReturn(false);

        repository.existsByNombreAndFincaId(NOMBRE, FINCA_ID);

        verify(jpaRepository, times(1)).existsByNombreAndFincaIdAndEliminadoEnIsNull(NOMBRE, FINCA_ID);
    }

    // ── Mapeo toDomain – todos los EstadoLote ────────────────────

    @Test
    void deberiaMapearCorrectamenteTodosLosEstadosDelLote() {
        for (EstadoLote estado : EstadoLote.values()) {
            LoteEntity entity = new LoteEntity();
            entity.setId(ID);
            entity.setNombre(NOMBRE);
            entity.setArea(AREA);
            entity.setEstado(estado);
            entity.setFincaId(FINCA_ID);

            when(jpaRepository.findById(ID)).thenReturn(Optional.of(entity));

            Optional<Lote> resultado = repository.findById(ID);

            assertTrue(resultado.isPresent());
            assertEquals(estado, resultado.get().getEstado());
        }
    }
    // ── Borrado lógico ───────────────────────────────────────────

    @Test
    void saveConservaLaFechaDeEliminacion() {
        LocalDateTime cuando = LocalDateTime.of(2026, 11, 7, 10, 30);
        Lote lote = new Lote(null, "Lote Norte", 1.5, FINCA_ID);
        lote.eliminar(cuando);
        when(jpaRepository.save(any(LoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Lote guardado = repository.save(lote);

        assertEquals(cuando, guardado.getEliminadoEn());
        assertTrue(guardado.estaEliminado());
    }
}
