-- Enlaces para recuperar la contraseña (se guarda solo el hash del token)
CREATE TABLE IF NOT EXISTS `restablecimientos_contrasena` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  `creado_en` datetime(6) NOT NULL,
  `expira_en` datetime(6) NOT NULL,
  `usado_en` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_restablecimiento_token` (`token_hash`),
  KEY `idx_restablecimiento_usuario` (`usuario_id`, `creado_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Qué correos opcionales quiere cada usuario (sin fila = recibe todos)
CREATE TABLE IF NOT EXISTS `preferencias_correo` (
  `usuario_id` bigint NOT NULL,
  `alertas_cultivo` bit(1) NOT NULL,
  `avisos_trabajadores` bit(1) NOT NULL,
  PRIMARY KEY (`usuario_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
