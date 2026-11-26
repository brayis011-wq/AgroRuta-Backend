-- La foto de perfil sale de la tabla usuarios.
--
-- Estaba en usuarios.foto_perfil (una imagen en texto, hasta unos 350 KB). El backend busca al
-- usuario en cada petición para validar la sesión, y con la foto en la misma fila la leía y la
-- traía completa cada vez aunque solo se muestra en la pantalla de perfil.
-- Ahora vive en su propia tabla y solo se lee cuando se pide el perfil.
--
-- usuarios.foto_perfil se deja vacía pero NO se borra todavía: durante el despliegue la versión
-- anterior del backend sigue corriendo unos segundos y fallaría si la columna desaparece.
-- Se puede quitar en una migración posterior.
--
-- Compatible con MySQL 8 y TiDB (un cambio por sentencia).

CREATE TABLE IF NOT EXISTS `fotos_perfil` (
  `usuario_id` bigint NOT NULL,
  `foto` mediumtext NOT NULL,
  PRIMARY KEY (`usuario_id`),
  CONSTRAINT `fk_foto_perfil_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `fotos_perfil` (`usuario_id`, `foto`)
SELECT u.`id`, u.`foto_perfil`
FROM `usuarios` u
WHERE u.`foto_perfil` IS NOT NULL
  AND u.`foto_perfil` <> ''
  AND NOT EXISTS (SELECT 1 FROM `fotos_perfil` f WHERE f.`usuario_id` = u.`id`);

UPDATE `usuarios` SET `foto_perfil` = NULL WHERE `foto_perfil` IS NOT NULL;
