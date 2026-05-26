package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.domain.Finca;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FincaRepositoryImplTest {

    @Mock
    private JpaFincaRepository jpaRepository;

    @InjectMocks
    private FincaRepositoryImpl repository;

    // ── Fixtures ─────────────────────────────────────────────────

    private static final Long          ID             = 1L;
    private static final Long          AGRICULTOR_ID  = 10L;
    private static final String        NOMBRE         = "Finca El Paraíso";
    private static final String        UBICACION      = "Cundinamarca";
    private static final Double        HECTAREAS      = 50.0;
    private static final LocalDateTime FECHA_REGISTRO = LocalDateTime.of(2023, 3, 15, 0, 0);
    private static final Double        LAT            = 4.7110;
    private static final Double        LNG            = -74.0721;

    private FincaEntity entityGuardada;
    private Finca       dominioEntrada;

    @BeforeEach
    void setUp() {
        entityGuardada = new FincaEntity();
        entityGuardada.setId(ID);
        entityGuardada.setNombre(NOMBRE);
        entityGuardada.setUbicacion(UBICACION);
        entityGuardada.setHectareas(HECTAREAS);
        entityGuardada.setAgricultorId(AGRICULTOR_ID);
        entityGuardada.setFechaRegistro(FECHA_REGISTRO);
        entityGuardada.setCentroideLat(LAT);
        entityGuardada.setCentroideLng(LNG);

        dominioEntrada = new Finca();
        dominioEntrada.setNombre(NOMBRE);
        dominioEntrada.setUbicacion(UBICACION);
        dominioEntrada.setHectareas(HECTAREAS);
        dominioEntrada.setAgricultorId(AGRICULTOR_ID);
        dominioEntrada.setFechaRegistro(FECHA_REGISTRO);
        dominioEntrada.setCentroideLat(LAT);
        dominioEntrada.setCentroideLng(LNG);
    }

    // ── save ─────────────────────────────────────────────────────

    @Test
    void deberiaSalvarYRetornarFincaConId() {
        when(jpaRepository.save(any(FincaEntity.class))).thenReturn(entityGuardada);

        Finca resultado = repository.save(dominioEntrada);

        assertEquals(ID,             resultado.getId());
        assertEquals(NOMBRE,         resultado.getNombre());
        assertEquals(UBICACION,      resultado.getUbicacion());
        assertEquals(HECTAREAS,      resultado.getHectareas());
        assertEquals(AGRICULTOR_ID,  resultado.getAgricultorId());
        assertEquals(FECHA_REGISTRO, resultado.getFechaRegistro());
        assertEquals(LAT,            resultado.getCentroideLat());
        assertEquals(LNG,            resultado.getCentroideLng());
        verify(jpaRepository, times(1)).save(any(FincaEntity.class));
    }

    @Test
    void saveDeberiaMapearCorrectamenteTodosLosCamposALaEntidad() {
        when(jpaRepository.save(any(FincaEntity.class))).thenReturn(entityGuardada);

        repository.save(dominioEntrada);

        verify(jpaRepository).save(argThat(entity ->
                entity.getNombre()       .equals(NOMBRE)         &&
                        entity.getUbicacion()    .equals(UBICACION)      &&
                        entity.getHectareas()    .equals(HECTAREAS)      &&
                        entity.getAgricultorId() .equals(AGRICULTOR_ID)  &&
                        entity.getFechaRegistro().equals(FECHA_REGISTRO) &&
                        entity.getCentroideLat() .equals(LAT)            &&
                        entity.getCentroideLng() .equals(LNG)
        ));
    }

    @Test
    void saveDeberiaPreservarIdCuandoSeActualizaUnaFinca() {
        dominioEntrada.setId(ID);
        when(jpaRepository.save(any(FincaEntity.class))).thenReturn(entityGuardada);

        Finca resultado = repository.save(dominioEntrada);

        assertEquals(ID, resultado.getId());
        verify(jpaRepository).save(argThat(entity -> ID.equals(entity.getId())));
    }

    @Test
    void deberiaGuardarFincaSinCentroide() {
        dominioEntrada.setCentroideLat(null);
        dominioEntrada.setCentroideLng(null);
        entityGuardada.setCentroideLat(null);
        entityGuardada.setCentroideLng(null);
        when(jpaRepository.save(any(FincaEntity.class))).thenReturn(entityGuardada);

        Finca resultado = repository.save(dominioEntrada);

        assertNull(resultado.getCentroideLat());
        assertNull(resultado.getCentroideLng());
    }

    // ── findById ─────────────────────────────────────────────────

    @Test
    void deberiaRetornarFincaCuandoExisteElId() {
        when(jpaRepository.findById(ID)).thenReturn(Optional.of(entityGuardada));

        Optional<Finca> resultado = repository.findById(ID);

        assertTrue(resultado.isPresent());
        assertEquals(ID,             resultado.get().getId());
        assertEquals(NOMBRE,         resultado.get().getNombre());
        assertEquals(UBICACION,      resultado.get().getUbicacion());
        assertEquals(HECTAREAS,      resultado.get().getHectareas());
        assertEquals(AGRICULTOR_ID,  resultado.get().getAgricultorId());
        assertEquals(FECHA_REGISTRO, resultado.get().getFechaRegistro());
        assertEquals(LAT,            resultado.get().getCentroideLat());
        assertEquals(LNG,            resultado.get().getCentroideLng());
    }

    @Test
    void deberiaRetornarVacioSiElIdNoExiste() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Finca> resultado = repository.findById(99L);

        assertFalse(resultado.isPresent());
    }

    @Test
    void findByIdDeberiaMapearCentroideNuloCorrectamente() {
        entityGuardada.setCentroideLat(null);
        entityGuardada.setCentroideLng(null);
        when(jpaRepository.findById(ID)).thenReturn(Optional.of(entityGuardada));

        Optional<Finca> resultado = repository.findById(ID);

        assertTrue(resultado.isPresent());
        assertNull(resultado.get().getCentroideLat());
        assertNull(resultado.get().getCentroideLng());
    }

    // ── findByAgricultorId ───────────────────────────────────────

    @Test
    void deberiaRetornarListaDeFincasPorAgricultorId() {
        FincaEntity entity2 = new FincaEntity();
        entity2.setId(2L);
        entity2.setNombre("Finca La Esperanza");
        entity2.setUbicacion("Boyacá");
        entity2.setHectareas(30.0);
        entity2.setAgricultorId(AGRICULTOR_ID);
        entity2.setFechaRegistro(FECHA_REGISTRO.plusMonths(2));

        when(jpaRepository.findByAgricultorId(AGRICULTOR_ID))
                .thenReturn(List.of(entityGuardada, entity2));

        List<Finca> resultado = repository.findByAgricultorId(AGRICULTOR_ID);

        assertEquals(2,                    resultado.size());
        assertEquals(NOMBRE,               resultado.get(0).getNombre());
        assertEquals("Finca La Esperanza", resultado.get(1).getNombre());
    }

    @Test
    void deberiaRetornarListaVaciaSiElAgricultorNoTieneFincas() {
        when(jpaRepository.findByAgricultorId(AGRICULTOR_ID)).thenReturn(List.of());

        List<Finca> resultado = repository.findByAgricultorId(AGRICULTOR_ID);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void findByAgricultorIdDeberiaMapearTodasLasFincasCorrectamente() {
        FincaEntity entity2 = new FincaEntity();
        entity2.setId(2L);
        entity2.setNombre("Finca B");
        entity2.setUbicacion("Tolima");
        entity2.setHectareas(15.0);
        entity2.setAgricultorId(AGRICULTOR_ID);
        entity2.setFechaRegistro(FECHA_REGISTRO);
        entity2.setCentroideLat(5.0);
        entity2.setCentroideLng(-75.0);

        when(jpaRepository.findByAgricultorId(AGRICULTOR_ID))
                .thenReturn(List.of(entityGuardada, entity2));

        List<Finca> resultado = repository.findByAgricultorId(AGRICULTOR_ID);

        assertEquals(AGRICULTOR_ID, resultado.get(0).getAgricultorId());
        assertEquals(AGRICULTOR_ID, resultado.get(1).getAgricultorId());
        assertEquals(5.0,           resultado.get(1).getCentroideLat());
        assertEquals(-75.0,         resultado.get(1).getCentroideLng());
    }

    // ── existsByNombreAndAgricultorId ─────────────────────────────

    @Test
    void deberiaRetornarTrueSiExisteFincaConMismoNombreYAgricultor() {
        when(jpaRepository.existsByNombreAndAgricultorId(NOMBRE, AGRICULTOR_ID))
                .thenReturn(true);

        assertTrue(repository.existsByNombreAndAgricultorId(NOMBRE, AGRICULTOR_ID));
    }

    @Test
    void deberiaRetornarFalseSiNoExisteFincaConEsaCombinacion() {
        when(jpaRepository.existsByNombreAndAgricultorId(NOMBRE, AGRICULTOR_ID))
                .thenReturn(false);

        assertFalse(repository.existsByNombreAndAgricultorId(NOMBRE, AGRICULTOR_ID));
    }

    @Test
    void existsDeberiaDelegarAlJpaConLosParametrosCorrectos() {
        when(jpaRepository.existsByNombreAndAgricultorId(any(), any())).thenReturn(false);

        repository.existsByNombreAndAgricultorId(NOMBRE, AGRICULTOR_ID);

        verify(jpaRepository, times(1))
                .existsByNombreAndAgricultorId(NOMBRE, AGRICULTOR_ID);
    }

    // ── deleteById ───────────────────────────────────────────────

    @Test
    void deberiaEliminarFincaPorId() {
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
}