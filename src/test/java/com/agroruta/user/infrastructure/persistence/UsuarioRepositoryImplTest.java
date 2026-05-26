package com.agroruta.user.infrastructure.persistence;

import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("UsuarioRepositoryImpl - Pruebas Unitarias")
class UsuarioRepositoryImplTest {

    @Mock
    private JpaUsuarioRepository jpaRepository;

    @InjectMocks
    private UsuarioRepositoryImpl repository;

    private Usuario usuarioBase;
    private UsuarioEntity entityBase;
    private LocalDateTime fechaCreacion;

    @BeforeEach
    void setUp() {
        fechaCreacion = LocalDateTime.of(2024, 1, 15, 10, 0);

        usuarioBase = new Usuario(1L, "Juan Pérez", "juan@agroruta.com", "password123", Rol.Agricultor);
        usuarioBase.setFechaCreacion(fechaCreacion);
        usuarioBase.setTelefono("3001234567");
        usuarioBase.setFotoPerfil("foto.jpg");

        entityBase = new UsuarioEntity(
                1L,
                "Juan Pérez",
                "juan@agroruta.com",
                "password123",
                Rol.Agricultor,
                true,
                fechaCreacion,
                "3001234567",
                "foto.jpg"
        );
    }

    // ══════════════════════════════════════════════════════════════════════
    //  save
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("Debe guardar y retornar el usuario activo mapeado al dominio")
        void debeGuardarYRetornarUsuarioMapeado() {
            when(jpaRepository.save(any(UsuarioEntity.class))).thenReturn(entityBase);

            Usuario resultado = repository.save(usuarioBase);

            assertNotNull(resultado);
            assertEquals(1L,                   resultado.getId());
            assertEquals("Juan Pérez",         resultado.getNombre());
            assertEquals("juan@agroruta.com",  resultado.getEmail());
            assertEquals(Rol.Agricultor,          resultado.getRol());
            assertEquals("3001234567",         resultado.getTelefono());
            assertEquals("foto.jpg",           resultado.getFotoPerfil());
            assertEquals(fechaCreacion,        resultado.getFechaCreacion());
            assertTrue(resultado.isActivo());
            verify(jpaRepository, times(1)).save(any(UsuarioEntity.class));
        }

        @Test
        @DisplayName("Debe guardar y retornar correctamente un usuario desactivado")
        void debeGuardarUsuarioDesactivado() {
            UsuarioEntity entityInactivo = new UsuarioEntity(
                    2L, "Ana López", "ana@agroruta.com", "pass456", Rol.Trabajador,
                    false, fechaCreacion, "3009876543", null
            );

            Usuario usuarioInactivo = new Usuario(2L, "Ana López", "ana@agroruta.com", "pass456", Rol.Trabajador);
            usuarioInactivo.desactivar();

            when(jpaRepository.save(any(UsuarioEntity.class))).thenReturn(entityInactivo);

            Usuario resultado = repository.save(usuarioInactivo);

            assertNotNull(resultado);
            assertEquals("Ana López", resultado.getNombre());
            assertFalse(resultado.isActivo());
            verify(jpaRepository, times(1)).save(any(UsuarioEntity.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  findById
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Debe retornar Optional con el usuario mapeado cuando el id existe")
        void debeRetornarOptionalConUsuarioSiIdExiste() {
            when(jpaRepository.findById(1L)).thenReturn(Optional.of(entityBase));

            Optional<Usuario> resultado = repository.findById(1L);

            assertTrue(resultado.isPresent());
            assertEquals(1L,                  resultado.get().getId());
            assertEquals("Juan Pérez",        resultado.get().getNombre());
            assertEquals("juan@agroruta.com", resultado.get().getEmail());
            assertEquals("3001234567",        resultado.get().getTelefono());
            assertEquals("foto.jpg",          resultado.get().getFotoPerfil());
            assertTrue(resultado.get().isActivo());
            verify(jpaRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Debe retornar Optional vacío cuando el id no existe")
        void debeRetornarOptionalVacioSiIdNoExiste() {
            when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

            Optional<Usuario> resultado = repository.findById(99L);

            assertTrue(resultado.isEmpty());
            verify(jpaRepository, times(1)).findById(99L);
        }

        @Test
        @DisplayName("Debe mapear correctamente un usuario inactivo encontrado por id")
        void debeMapearUsuarioInactivoEncontradoPorId() {
            UsuarioEntity entityInactivo = new UsuarioEntity(
                    1L, "Juan Pérez", "juan@agroruta.com", "password123", Rol.Agricultor,
                    false, fechaCreacion, "3001234567", "foto.jpg"
            );
            when(jpaRepository.findById(1L)).thenReturn(Optional.of(entityInactivo));

            Optional<Usuario> resultado = repository.findById(1L);

            assertTrue(resultado.isPresent());
            assertFalse(resultado.get().isActivo());
            verify(jpaRepository, times(1)).findById(1L);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  findByEmail
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("findByEmail()")
    class FindByEmail {

        @Test
        @DisplayName("Debe retornar Optional con el usuario mapeado cuando el email existe")
        void debeRetornarOptionalConUsuarioSiEmailExiste() {
            when(jpaRepository.findByEmail("juan@agroruta.com")).thenReturn(Optional.of(entityBase));

            Optional<Usuario> resultado = repository.findByEmail("juan@agroruta.com");

            assertTrue(resultado.isPresent());
            assertEquals("Juan Pérez",        resultado.get().getNombre());
            assertEquals("juan@agroruta.com", resultado.get().getEmail());
            assertEquals(Rol.Agricultor,        resultado.get().getRol());
            assertTrue(resultado.get().isActivo());
            verify(jpaRepository, times(1)).findByEmail("juan@agroruta.com");
        }

        @Test
        @DisplayName("Debe retornar Optional vacío cuando el email no está registrado")
        void debeRetornarOptionalVacioSiEmailNoExiste() {
            when(jpaRepository.findByEmail("noexiste@agroruta.com")).thenReturn(Optional.empty());

            Optional<Usuario> resultado = repository.findByEmail("noexiste@agroruta.com");

            assertTrue(resultado.isEmpty());
            verify(jpaRepository, times(1)).findByEmail("noexiste@agroruta.com");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  findAllActivos
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("findAllActivos()")
    class FindAllActivos {

        @Test
        @DisplayName("Debe retornar todos los usuarios activos mapeados al dominio")
        void debeRetornarTodosLosUsuariosActivos() {
            UsuarioEntity segundaEntity = new UsuarioEntity(
                    2L, "Ana López", "ana@agroruta.com", "pass456", Rol.Trabajador,
                    true, fechaCreacion, "3009876543", null
            );

            when(jpaRepository.findByActivoTrue()).thenReturn(List.of(entityBase, segundaEntity));

            List<Usuario> resultado = repository.findAllActivos();

            assertEquals(2,                   resultado.size());
            assertEquals("Juan Pérez",        resultado.get(0).getNombre());
            assertEquals("Ana López",         resultado.get(1).getNombre());
            assertTrue(resultado.get(0).isActivo());
            assertTrue(resultado.get(1).isActivo());
            verify(jpaRepository, times(1)).findByActivoTrue();
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay usuarios activos")
        void debeRetornarListaVaciaSiNoHayActivos() {
            when(jpaRepository.findByActivoTrue()).thenReturn(List.of());

            List<Usuario> resultado = repository.findAllActivos();

            assertTrue(resultado.isEmpty());
            verify(jpaRepository, times(1)).findByActivoTrue();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Mapeo de campos opcionales (telefono, fotoPerfil, fechaCreacion)
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Mapeo de campos opcionales")
    class MapeoOpcionales {

        @Test
        @DisplayName("Debe mapear correctamente un usuario sin teléfono ni fotoPerfil")
        void debeMapearUsuarioConCamposNulos() {
            UsuarioEntity entitySinOpcionales = new UsuarioEntity(
                    3L, "Pedro Ruiz", "pedro@agroruta.com", "pass789", Rol.Administrador,
                    true, fechaCreacion, null, null
            );
            when(jpaRepository.findById(3L)).thenReturn(Optional.of(entitySinOpcionales));

            Optional<Usuario> resultado = repository.findById(3L);

            assertTrue(resultado.isPresent());
            assertNull(resultado.get().getTelefono());
            assertNull(resultado.get().getFotoPerfil());
            verify(jpaRepository, times(1)).findById(3L);
        }

        @Test
        @DisplayName("Debe preservar la fechaCreacion original al mapear de entidad a dominio")
        void debePreservarFechaCreacion() {
            when(jpaRepository.findById(1L)).thenReturn(Optional.of(entityBase));

            Optional<Usuario> resultado = repository.findById(1L);

            assertTrue(resultado.isPresent());
            assertEquals(fechaCreacion, resultado.get().getFechaCreacion());
            verify(jpaRepository, times(1)).findById(1L);
        }
    }
}