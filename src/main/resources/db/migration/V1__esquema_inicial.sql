-- ============================================================================
-- V1: esquema inicial de AgroRuta
-- Copia de la estructura de producción (TiDB) del 2026-11-05, sacada con la
-- "Copia diaria" de GitHub. Usa CREATE TABLE IF NOT EXISTS: en una base que ya
-- tiene las tablas no cambia nada; en una base vacía la deja lista.
-- Las tablas van en orden para que cada clave foránea apunte a una que ya existe.
-- ============================================================================

CREATE TABLE IF NOT EXISTS `usuarios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activo` bit(1) NOT NULL,
  `email` varchar(150) NOT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `foto_perfil` mediumtext DEFAULT NULL,
  `nombre` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `rol` enum('Administrador','Agricultor','Trabajador') NOT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKkfsp0s1tflm1cwlj8idhqsad0` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tipos_cultivo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activo` bit(1) NOT NULL,
  `nombre` varchar(80) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK2bkw9xd086nmubeoyr3l9tjyb` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `variedades_cultivo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activo` bit(1) NOT NULL,
  `nombre` varchar(80) NOT NULL,
  `tipo_cultivo_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5bjl8jfe3ov9lq9oid6f9jxwk` (`tipo_cultivo_id`,`nombre`),
  CONSTRAINT `FKmk7pwrc8byr1m0clu583u7hqs` FOREIGN KEY (`tipo_cultivo_id`) REFERENCES `tipos_cultivo` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `fincas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agricultor_id` bigint NOT NULL,
  `centroide_lat` double DEFAULT NULL,
  `centroide_lng` double DEFAULT NULL,
  `fecha_registro` datetime(6) NOT NULL,
  `hectareas` double NOT NULL,
  `nombre` varchar(255) NOT NULL,
  `ubicacion` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_finca_nombre_agricultor` (`nombre`,`agricultor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `lotes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `area` double NOT NULL,
  `centroide_lat` double DEFAULT NULL,
  `centroide_lng` double DEFAULT NULL,
  `coordenadas` text DEFAULT NULL,
  `estado` enum('DISPONIBLE','EN_CULTIVO','EN_DESCANSO') NOT NULL,
  `finca_id` bigint NOT NULL,
  `nombre` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lote_nombre_finca` (`nombre`,`finca_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `siembras` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cantidad_plantas` int NOT NULL,
  `estado_cultivo` enum('COSECHA','CRECIMIENTO','FINALIZADO','GERMINACION','PRODUCCION') NOT NULL,
  `fecha_siembra` date NOT NULL,
  `lote_id` bigint NOT NULL,
  `variedad` varchar(80) NOT NULL,
  `tipo_cultivo` varchar(80) DEFAULT NULL,
  `variedad_id` bigint DEFAULT NULL,
  `fecha_fin` date DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `siembra_etapas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `etapa` enum('COSECHA','CRECIMIENTO','FINALIZADO','GERMINACION','PRODUCCION') NOT NULL,
  `fecha` date NOT NULL,
  `siembra_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `actividades_cultivo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `descripcion` varchar(255) NOT NULL,
  `fecha` date NOT NULL,
  `siembra_id` bigint NOT NULL,
  `tipo` enum('DESHIERBE','OTRO','PODA','REVISION','RIEGO','TUTOREO') NOT NULL,
  `costo` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_actividad_tipo_fecha_siembra` (`tipo`,`fecha`,`siembra_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `cosechas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `calidad` enum('PRIMERA','SEGUNDA','TERCERA') NOT NULL,
  `cantidad_kg` double NOT NULL,
  `fecha` date NOT NULL,
  `observaciones` varchar(255) DEFAULT NULL,
  `siembra_id` bigint NOT NULL,
  `precio_kg` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cosecha_fecha_siembra` (`fecha`,`siembra_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `agricultural_inputs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activo` bit(1) NOT NULL,
  `creado_en` datetime(6) NOT NULL,
  `dosis_sugerida` double NOT NULL,
  `nombre` varchar(150) NOT NULL,
  `reentrada_horas` int NOT NULL,
  `tipo` enum('ACARICIDA','COADYUVANTE','FERTILIZANTE_FOLIAR','FERTILIZANTE_SUELO','FUNGICIDA','HERBICIDA','INSECTICIDA','NEMATICIDA','REGULADOR_CRECIMIENTO') NOT NULL,
  `unidad_sugerida` enum('GRAMOS','KG','LITROS','ML') NOT NULL,
  `dias_carencia` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `fumigaciones` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `area_aplicada` double NOT NULL,
  `dosis` double NOT NULL,
  `fecha` date NOT NULL,
  `observaciones` varchar(255) DEFAULT NULL,
  `producto` varchar(255) NOT NULL,
  `siembra_id` bigint NOT NULL,
  `unidad_medida` enum('GRAMOS','KG','LITROS','ML') NOT NULL,
  `agricultural_input_id` bigint DEFAULT NULL,
  `dias_carencia` int DEFAULT NULL,
  `reentrada_horas` int DEFAULT NULL,
  `costo` double DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `cargos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activo` bit(1) NOT NULL,
  `descripcion` varchar(255) DEFAULT NULL,
  `nombre` varchar(100) NOT NULL,
  `valor_jornal` decimal(10,2) NOT NULL,
  `agricultor_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cargo_nombre_agricultor` (`nombre`,`agricultor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `trabajadores` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `apellido` varchar(100) NOT NULL,
  `cedula` varchar(20) NOT NULL,
  `direccion` varchar(255) DEFAULT NULL,
  `estado` enum('ACTIVO','INACTIVO','SUSPENDIDO') NOT NULL,
  `fecha_ingreso` date NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `tipo_contrato` enum('FIJO','JORNAL','TEMPORAL') NOT NULL,
  `cargo_id` bigint NOT NULL,
  `agricultor_id` bigint DEFAULT NULL,
  `acceso_email` varchar(150) DEFAULT NULL,
  `acceso_estado` enum('APROBADO','PENDIENTE','RECHAZADO') DEFAULT NULL,
  `acceso_nombre` varchar(100) DEFAULT NULL,
  `usuario_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5yn53m1kktaocss6wwvbvksew` (`cedula`),
  KEY `FKf6waf5cmxntf1bhoeruvpmsfs` (`cargo_id`),
  CONSTRAINT `FKf6waf5cmxntf1bhoeruvpmsfs` FOREIGN KEY (`cargo_id`) REFERENCES `cargos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `actividades` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activa` bit(1) NOT NULL,
  `descripcion` varchar(255) DEFAULT NULL,
  `nombre` varchar(100) NOT NULL,
  `agricultor_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `jornales` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cultivo_id` bigint NOT NULL,
  `fecha` date NOT NULL,
  `liquidado` bit(1) NOT NULL,
  `nombre_cultivo` varchar(150) DEFAULT NULL,
  `observaciones` varchar(500) DEFAULT NULL,
  `valor_jornal` decimal(10,2) NOT NULL,
  `trabajador_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKb5fc3xwb3r01bpwrot9pw9c5m` (`trabajador_id`),
  CONSTRAINT `FKb5fc3xwb3r01bpwrot9pw9c5m` FOREIGN KEY (`trabajador_id`) REFERENCES `trabajadores` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `jornal_actividades` (
  `jornal_id` bigint NOT NULL,
  `actividad_id` bigint NOT NULL,
  KEY `FK76vdblloal5qbp726l1lpqivt` (`actividad_id`),
  KEY `FKa8eoiyux75aqm6lixrmbkybbv` (`jornal_id`),
  CONSTRAINT `FK76vdblloal5qbp726l1lpqivt` FOREIGN KEY (`actividad_id`) REFERENCES `actividades` (`id`),
  CONSTRAINT `FKa8eoiyux75aqm6lixrmbkybbv` FOREIGN KEY (`jornal_id`) REFERENCES `jornales` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `nominas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `estado` enum('ANULADA','APROBADA','PAGADA','PENDIENTE') NOT NULL,
  `fecha_generacion` date NOT NULL,
  `observaciones` varchar(500) DEFAULT NULL,
  `periodo_fin` date NOT NULL,
  `periodo_inicio` date NOT NULL,
  `total_jornales` int NOT NULL,
  `valor_total` decimal(12,2) NOT NULL,
  `trabajador_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKcapv6uxmib4vboarnr6tdoat3` (`trabajador_id`),
  CONSTRAINT `FKcapv6uxmib4vboarnr6tdoat3` FOREIGN KEY (`trabajador_id`) REFERENCES `trabajadores` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `nomina_jornales` (
  `nomina_id` bigint NOT NULL,
  `jornal_id` bigint NOT NULL,
  UNIQUE KEY `UKpprxxsk8ysq2ynyrfdhuu4a22` (`jornal_id`),
  KEY `FK67vwwxsm40wh6q4fj6l7r3rvf` (`nomina_id`),
  CONSTRAINT `FK7uo3y0n2mgxr46piyeebvgil4` FOREIGN KEY (`jornal_id`) REFERENCES `jornales` (`id`),
  CONSTRAINT `FK67vwwxsm40wh6q4fj6l7r3rvf` FOREIGN KEY (`nomina_id`) REFERENCES `nominas` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `pagos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comprobante` varchar(100) DEFAULT NULL,
  `fecha_pago` date NOT NULL,
  `metodo_pago` enum('CHEQUE','EFECTIVO','TRANSFERENCIA') NOT NULL,
  `monto` decimal(12,2) NOT NULL,
  `observaciones` varchar(500) DEFAULT NULL,
  `nomina_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKgb357hmyw9v99g2jf4bve9kyb` (`nomina_id`),
  CONSTRAINT `FKgb357hmyw9v99g2jf4bve9kyb` FOREIGN KEY (`nomina_id`) REFERENCES `nominas` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
