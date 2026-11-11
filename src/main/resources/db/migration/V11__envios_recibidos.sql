-- Modo sin conexión: el teléfono guarda el registro y lo reenvía cuando vuelve la señal.
-- Cada envío trae una clave propia; si la respuesta se perdió en el camino y el teléfono
-- lo manda otra vez, la clave repetida evita que el jornal o la cosecha queden duplicados.
CREATE TABLE IF NOT EXISTS `envios_recibidos` (
  `usuario_id` bigint NOT NULL,
  `clave` varchar(64) NOT NULL,
  `recibido_en` datetime(6) NOT NULL,
  PRIMARY KEY (`usuario_id`, `clave`),
  KEY `idx_envio_recibido_en` (`recibido_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
