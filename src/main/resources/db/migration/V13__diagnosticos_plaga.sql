-- Diagnósticos de plagas y enfermedades hechos a partir de una foto (bitácora de la siembra)
CREATE TABLE IF NOT EXISTS `diagnosticos_plaga` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `siembra_id` bigint NOT NULL,
  `usuario_id` bigint NOT NULL,
  `creado_en` datetime(6) NOT NULL,
  -- Copia reducida de la foto (JPEG de unos 640 px, sin datos de ubicación)
  `foto` mediumblob NOT NULL,
  `nota` varchar(300) DEFAULT NULL,
  `problema` varchar(160) NOT NULL,
  `tipo` varchar(20) NOT NULL,
  `confianza` varchar(10) NOT NULL,
  `sintomas` varchar(1200) DEFAULT NULL,
  -- Una recomendación o posibilidad por línea
  `recomendaciones` text,
  `otras_posibilidades` text,
  `advertencia` varchar(600) DEFAULT NULL,
  `modelo` varchar(60) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_diagnostico_siembra` (`siembra_id`, `creado_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Fotos analizadas por persona y día: cada análisis cuesta, así que hay un tope diario
CREATE TABLE IF NOT EXISTS `diagnosticos_uso` (
  `usuario_id` bigint NOT NULL,
  `dia` date NOT NULL,
  `cantidad` int NOT NULL,
  PRIMARY KEY (`usuario_id`, `dia`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
