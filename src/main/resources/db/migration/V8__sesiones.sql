-- Sesiones renovables (refresh tokens). El navegador guarda el token en una cookie httpOnly;
-- aquí solo queda su hash. Cada renovación marca la sesión como rotada y crea una nueva.
--
-- Compatible con MySQL 8 y TiDB.

CREATE TABLE IF NOT EXISTS `sesiones` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  `creado_en` datetime(6) NOT NULL,
  `expira_en` datetime(6) NOT NULL,
  `rotado_en` datetime(6) DEFAULT NULL,
  `revocado_en` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sesion_token` (`token_hash`),
  KEY `idx_sesion_usuario` (`usuario_id`, `expira_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
