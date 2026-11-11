-- Sesiones abiertas: cada inicio de sesión es una "familia" que se conserva al renovar el token,
-- para poder mostrarle a la persona en qué dispositivos tiene la sesión abierta y cerrarlas.
-- Segundo factor: código de 6 dígitos de una app de autenticación (TOTP), opcional por cuenta.
--
-- Compatible con MySQL 8 y TiDB (un cambio por sentencia).

ALTER TABLE `sesiones` ADD COLUMN `familia` varchar(36) DEFAULT NULL;
ALTER TABLE `sesiones` ADD COLUMN `dispositivo` varchar(120) DEFAULT NULL;
ALTER TABLE `sesiones` ADD COLUMN `iniciada_en` datetime(6) DEFAULT NULL;

-- Las sesiones que ya existían: cada una queda como su propia familia
UPDATE `sesiones` SET `familia` = CONCAT('v', `id`), `iniciada_en` = `creado_en` WHERE `familia` IS NULL;

ALTER TABLE `sesiones` ADD KEY `idx_sesion_familia` (`usuario_id`, `familia`);

CREATE TABLE IF NOT EXISTS `segundo_factor` (
  `usuario_id` bigint NOT NULL,
  -- Secreto de la app de autenticación, cifrado (AES-GCM); nunca en claro
  `secreto` varchar(255) NOT NULL,
  `activo` bit(1) NOT NULL DEFAULT b'0',
  -- Hashes (SHA-256) de los códigos de respaldo que quedan, separados por coma
  `respaldo` varchar(700) DEFAULT NULL,
  -- Último intervalo de 30 s aceptado: un código no sirve dos veces
  `ultimo_paso` bigint DEFAULT NULL,
  `creado_en` datetime(6) NOT NULL,
  `activado_en` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`usuario_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
