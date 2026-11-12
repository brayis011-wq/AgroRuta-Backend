-- Avisos push (Web Push): cada navegador o teléfono donde la persona activó los avisos
CREATE TABLE IF NOT EXISTS `suscripciones_push` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint NOT NULL,
  -- Dirección que entrega el servicio de avisos del navegador (Google, Mozilla, Apple, Microsoft)
  `endpoint` varchar(700) NOT NULL,
  -- SHA-256 del endpoint: un navegador solo puede estar suscrito una vez
  `endpoint_hash` char(64) NOT NULL,
  -- Claves públicas del navegador con las que se cifra cada aviso
  `p256dh` varchar(120) NOT NULL,
  `auth` varchar(40) NOT NULL,
  `dispositivo` varchar(120) DEFAULT NULL,
  `creada_en` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_suscripcion_push_endpoint` (`endpoint_hash`),
  KEY `idx_suscripcion_push_usuario` (`usuario_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Par de claves con el que el servidor se identifica ante los servicios de avisos (VAPID).
-- Se genera solo la primera vez; la privada se guarda cifrada.
CREATE TABLE IF NOT EXISTS `claves_push` (
  `id` tinyint NOT NULL,
  `publica` varchar(120) NOT NULL,
  `privada` varchar(255) NOT NULL,
  `creada_en` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
