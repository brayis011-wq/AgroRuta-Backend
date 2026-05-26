package com.agroruta.configuration.infrastructure.persistence;

import com.agroruta.configuration.domain.Perfil;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.infrastructure.persistence.JpaUsuarioRepository;
import com.agroruta.user.infrastructure.persistence.UsuarioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link ProfileRepositoryAdapter}.
 *
 * Qué se verifica aquí:
 *  - Mapeo correcto entre UsuarioEntity ↔ Perfil (dominio).
 *  - Delegación exacta al puerto secundario JpaUsuarioRepository.
 *  - Manejo de Optional vacío → RuntimeException en cada método.
 *  - Que los campos modificados en guardar() y cambiarPassword()
 *    se aplican sobre la entidad antes de persistir.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileRepositoryAdapter")
class ProfileRepositoryAdapterTest {

    @Mock
    private JpaUsuarioRepository jpaUsuarioRepository;

    @InjectMocks
    private ProfileRepositoryAdapter adapter;

    // ── fixture ──────────────────────────────────────────────────────────────
    private UsuarioEntity entityEjemplo;

    @BeforeEach
    void configurarFixture() {
        entityEjemplo = new UsuarioEntity();
        entityEjemplo.setId(1L);
        entityEjemplo.setNombre("Ana Gómez");
        entityEjemplo.setEmail("ana@agroruta.com");
        entityEjemplo.setTelefono("3001234567");
        entityEjemplo.setFotoPerfil("https://cdn.agroruta.com/foto.jpg");
        entityEjemplo.setPassword("$2a$10$hash");
        entityEjemplo.setRol(Rol.Agricultor);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // buscarPorEmail
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorEmail")
    class BuscarPorEmail {

        @Test
        @DisplayName("debería retornar Optional con Perfil mapeado cuando el email existe")
        void deberiaRetornarPerfil_cuandoEmailExiste() {
            when(jpaUsuarioRepository.findByEmail("ana@agroruta.com"))
                    .thenReturn(Optional.of(entityEjemplo));

            Optional<Perfil> resultado = adapter.buscarPorEmail("ana@agroruta.com");

            assertThat(resultado).isPresent();
            Perfil perfil = resultado.get();
            assertThat(perfil.getId()).isEqualTo(1L);
            assertThat(perfil.getNombre()).isEqualTo("Ana Gómez");
            assertThat(perfil.getEmail()).isEqualTo("ana@agroruta.com");
            assertThat(perfil.getTelefono()).isEqualTo("3001234567");
            assertThat(perfil.getFotoPerfil()).isEqualTo("https://cdn.agroruta.com/foto.jpg");
            assertThat(perfil.getRol()).isEqualTo(Rol.Agricultor);
            verify(jpaUsuarioRepository, times(1)).findByEmail("ana@agroruta.com");
        }

        @Test
        @DisplayName("debería retornar Optional vacío cuando el email no existe")
        void deberiaRetornarEmpty_cuandoEmailNoExiste() {
            when(jpaUsuarioRepository.findByEmail("nadie@agroruta.com"))
                    .thenReturn(Optional.empty());

            Optional<Perfil> resultado = adapter.buscarPorEmail("nadie@agroruta.com");

            assertThat(resultado).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // buscarPorId
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("debería retornar Optional con Perfil mapeado cuando el id existe")
        void deberiaRetornarPerfil_cuandoIdExiste() {
            when(jpaUsuarioRepository.findById(1L))
                    .thenReturn(Optional.of(entityEjemplo));

            Optional<Perfil> resultado = adapter.buscarPorId(1L);

            assertThat(resultado).isPresent();
            assertThat(resultado.get().getId()).isEqualTo(1L);
            assertThat(resultado.get().getEmail()).isEqualTo("ana@agroruta.com");
            verify(jpaUsuarioRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("debería retornar Optional vacío cuando el id no existe")
        void deberiaRetornarEmpty_cuandoIdNoExiste() {
            when(jpaUsuarioRepository.findById(99L))
                    .thenReturn(Optional.empty());

            Optional<Perfil> resultado = adapter.buscarPorId(99L);

            assertThat(resultado).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // guardar
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("guardar")
    class Guardar {

        @Test
        @DisplayName("debería actualizar los campos de la entidad y retornar el Perfil guardado")
        void deberiaActualizarEntityYRetornarPerfil() {
            Perfil perfilActualizado = new Perfil(
                    1L, "Ana Nueva", "ana@agroruta.com",
                    "3109876543", "https://cdn.agroruta.com/nueva.jpg", Rol.Agricultor
            );

            // findById devuelve la entidad original
            when(jpaUsuarioRepository.findById(1L)).thenReturn(Optional.of(entityEjemplo));
            // save devuelve la entidad con los nuevos valores
            UsuarioEntity entityActualizada = new UsuarioEntity();
            entityActualizada.setId(1L);
            entityActualizada.setNombre("Ana Nueva");
            entityActualizada.setEmail("ana@agroruta.com");
            entityActualizada.setTelefono("3109876543");
            entityActualizada.setFotoPerfil("https://cdn.agroruta.com/nueva.jpg");
            entityActualizada.setRol(Rol.Agricultor);
            when(jpaUsuarioRepository.save(any(UsuarioEntity.class))).thenReturn(entityActualizada);

            Perfil resultado = adapter.guardar(perfilActualizado);

            // verificamos que los campos se aplicaron sobre la entidad antes de persistir
            ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
            verify(jpaUsuarioRepository).save(captor.capture());
            UsuarioEntity entityCapturada = captor.getValue();
            assertThat(entityCapturada.getNombre()).isEqualTo("Ana Nueva");
            assertThat(entityCapturada.getTelefono()).isEqualTo("3109876543");
            assertThat(entityCapturada.getFotoPerfil()).isEqualTo("https://cdn.agroruta.com/nueva.jpg");

            // y que el retorno está correctamente mapeado
            assertThat(resultado.getNombre()).isEqualTo("Ana Nueva");
            assertThat(resultado.getTelefono()).isEqualTo("3109876543");
        }

        @Test
        @DisplayName("debería lanzar RuntimeException cuando el usuario no existe al guardar")
        void deberiaLanzarExcepcion_cuandoUsuarioNoExisteAlGuardar() {
            Perfil perfilInexistente = new Perfil(
                    99L, "X", "x@x.com", "000", "url", Rol.Agricultor
            );
            when(jpaUsuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.guardar(perfilInexistente))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Usuario no encontrado");

            verify(jpaUsuarioRepository, never()).save(any());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // cambiarPassword
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("cambiarPassword")
    class CambiarPassword {

        @Test
        @DisplayName("debería actualizar la contraseña en la entidad y persistir")
        void deberiaActualizarPasswordYPersistir() {
            String nuevoHash = "$2a$10$nuevo_hash";
            when(jpaUsuarioRepository.findById(1L)).thenReturn(Optional.of(entityEjemplo));
            when(jpaUsuarioRepository.save(any(UsuarioEntity.class))).thenReturn(entityEjemplo);

            adapter.cambiarPassword(1L, nuevoHash);

            ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
            verify(jpaUsuarioRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo(nuevoHash);
        }

        @Test
        @DisplayName("debería lanzar RuntimeException cuando el usuario no existe al cambiar contraseña")
        void deberiaLanzarExcepcion_cuandoUsuarioNoExisteAlCambiarPassword() {
            when(jpaUsuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.cambiarPassword(99L, "cualquierHash"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Usuario no encontrado");

            verify(jpaUsuarioRepository, never()).save(any());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // obtenerPasswordHash
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("obtenerPasswordHash")
    class ObtenerPasswordHash {

        @Test
        @DisplayName("debería retornar el hash cuando el usuario existe")
        void deberiaRetornarHash_cuandoUsuarioExiste() {
            when(jpaUsuarioRepository.findById(1L)).thenReturn(Optional.of(entityEjemplo));

            String hash = adapter.obtenerPasswordHash(1L);

            assertThat(hash).isEqualTo("$2a$10$hash");
            verify(jpaUsuarioRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("debería lanzar RuntimeException cuando el usuario no existe")
        void deberiaLanzarExcepcion_cuandoUsuarioNoExiste() {
            when(jpaUsuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.obtenerPasswordHash(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Usuario no encontrado");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // toDomain — mapeo (verificado indirectamente en cada método)
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("toDomain (mapeo de campos)")
    class ToDomain {

        @Test
        @DisplayName("debería mapear todos los campos de UsuarioEntity a Perfil correctamente")
        void deberiaMappearTodosLosCampos() {
            when(jpaUsuarioRepository.findById(1L)).thenReturn(Optional.of(entityEjemplo));

            Perfil perfil = adapter.buscarPorId(1L).orElseThrow();

            assertThat(perfil.getId()).isEqualTo(entityEjemplo.getId());
            assertThat(perfil.getNombre()).isEqualTo(entityEjemplo.getNombre());
            assertThat(perfil.getEmail()).isEqualTo(entityEjemplo.getEmail());
            assertThat(perfil.getTelefono()).isEqualTo(entityEjemplo.getTelefono());
            assertThat(perfil.getFotoPerfil()).isEqualTo(entityEjemplo.getFotoPerfil());
            assertThat(perfil.getRol()).isEqualTo(entityEjemplo.getRol());
        }

        @Test
        @DisplayName("debería mapear campos nulos sin lanzar excepción")
        void deberiaMappearCamposNulos() {
            entityEjemplo.setTelefono(null);
            entityEjemplo.setFotoPerfil(null);
            when(jpaUsuarioRepository.findById(1L)).thenReturn(Optional.of(entityEjemplo));

            Perfil perfil = adapter.buscarPorId(1L).orElseThrow();

            assertThat(perfil.getTelefono()).isNull();
            assertThat(perfil.getFotoPerfil()).isNull();
        }
    }
}