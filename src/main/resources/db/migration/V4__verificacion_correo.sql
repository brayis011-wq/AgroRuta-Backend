-- Las cuentas que ya existían cuentan como confirmadas; las nuevas empiezan sin confirmar
ALTER TABLE `usuarios` ADD COLUMN `email_verificado` bit(1) NOT NULL DEFAULT b'1';

-- Enlaces para confirmar el correo al registrarse (se guarda solo el hash del token)
CREATE TABLE IF NOT EXISTS `verificaciones_correo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  `creado_en` datetime(6) NOT NULL,
  `expira_en` datetime(6) NOT NULL,
  `usado_en` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_verificacion_token` (`token_hash`),
  KEY `idx_verificacion_usuario` (`usuario_id`, `creado_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
