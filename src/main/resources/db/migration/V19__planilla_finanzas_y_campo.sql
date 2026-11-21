-- ─────────────────────────────────────────────────────────────────────────────
-- V19: funciones nuevas de trabajo, dinero y campo.
--
-- 35. Pago al kilo: el cargo dice si se paga por día o por kilo; el jornal guarda los
--     kilos recogidos y el valor del kilo de ese día (si luego cambia el cargo, el jornal
--     ya registrado no cambia, igual que pasa con el valor del día).
-- 37. Precios de referencia: el promedio nacional semanal de SIPSA (DANE) por producto,
--     y el precio que cada agricultor fija para lo que SIPSA no publica (café, cacao...).
-- 38. Gastos generales de la finca (transporte, herramientas, servicios).
-- 39. Lluvia por finca y día.
-- 40. Actividades que se repiten cada N días en una siembra.
-- 42. Ventas a crédito y sus abonos.
-- 43. Presupuesto por temporada (siembra).
--
-- Solo agrega columnas con valor por defecto y tablas nuevas: la versión anterior del
-- backend sigue funcionando sobre este esquema mientras se despliega la nueva.
-- Compatible con MySQL 8 y TiDB.
-- ─────────────────────────────────────────────────────────────────────────────

-- 35. Pago al kilo
ALTER TABLE cargos ADD COLUMN modalidad_pago varchar(10) NOT NULL DEFAULT 'JORNAL';
ALTER TABLE cargos ADD COLUMN valor_kilo decimal(10,2) DEFAULT NULL;
ALTER TABLE jornales ADD COLUMN kilos decimal(10,2) DEFAULT NULL;
ALTER TABLE jornales ADD COLUMN valor_kilo decimal(10,2) DEFAULT NULL;

-- 34. La planilla semanal pide los jornales de todo el equipo entre dos fechas
CREATE INDEX idx_jornal_fecha_trabajador ON jornales (fecha, trabajador_id);

-- 37. Precios de referencia
CREATE TABLE IF NOT EXISTS `precios_referencia` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  -- Nombre del producto tal como lo publica SIPSA ("Aguacate Hass", "Uchuva con cáscara")
  `producto` varchar(120) NOT NULL,
  -- Primer día de la semana publicada
  `semana` date NOT NULL,
  -- Promedio de los precios mayoristas de todos los mercados que reportaron (pesos por kilo)
  `precio_kg` decimal(14,2) NOT NULL,
  `minimo_kg` decimal(14,2) DEFAULT NULL,
  `maximo_kg` decimal(14,2) DEFAULT NULL,
  `mercados` int NOT NULL,
  `actualizado_en` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_precio_referencia_producto_semana` (`producto`, `semana`),
  KEY `idx_precio_referencia_semana` (`semana`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `precios_propios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agricultor_id` bigint NOT NULL,
  -- Nombre del cultivo ("Café"), el mismo que guarda la siembra
  `cultivo` varchar(80) NOT NULL,
  -- Producto de SIPSA elegido para este cultivo (si la búsqueda automática no acertó)
  `producto` varchar(120) DEFAULT NULL,
  -- Precio fijado a mano (para lo que SIPSA no publica); manda sobre el producto
  `precio_kg` decimal(14,2) DEFAULT NULL,
  `actualizado_en` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_precio_propio_cultivo` (`agricultor_id`, `cultivo`),
  CONSTRAINT `fk_precio_propio_agricultor` FOREIGN KEY (`agricultor_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 38. Gastos generales de la finca
CREATE TABLE IF NOT EXISTS `gastos_finca` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `finca_id` bigint NOT NULL,
  `fecha` date NOT NULL,
  -- TRANSPORTE, HERRAMIENTAS, SERVICIOS, MANTENIMIENTO, ADMINISTRACION, IMPUESTOS, OTRO
  `categoria` varchar(20) NOT NULL,
  `descripcion` varchar(255) NOT NULL,
  `monto` decimal(14,2) NOT NULL,
  `creado_en` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_gasto_finca_fecha` (`finca_id`, `fecha`),
  CONSTRAINT `fk_gasto_finca` FOREIGN KEY (`finca_id`) REFERENCES `fincas` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 39. Lluvia (milímetros caídos en el pluviómetro de la finca, un dato por día)
CREATE TABLE IF NOT EXISTS `lluvias` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `finca_id` bigint NOT NULL,
  `fecha` date NOT NULL,
  `milimetros` decimal(6,1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lluvia_finca_fecha` (`finca_id`, `fecha`),
  CONSTRAINT `fk_lluvia_finca` FOREIGN KEY (`finca_id`) REFERENCES `fincas` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 40. Actividades que se repiten
CREATE TABLE IF NOT EXISTS `actividades_programadas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `siembra_id` bigint NOT NULL,
  -- Un tipo de labor (PODA, RIEGO...) o FERTILIZACION / FUMIGACION
  `tipo` varchar(20) NOT NULL,
  `descripcion` varchar(255) DEFAULT NULL,
  `cada_dias` int NOT NULL,
  -- Primera vez que toca
  `desde` date NOT NULL,
  -- Última fecha en que puede tocar (null: mientras dure la siembra)
  `hasta` date DEFAULT NULL,
  `creado_en` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_programada_siembra` (`siembra_id`),
  CONSTRAINT `fk_programada_siembra` FOREIGN KEY (`siembra_id`) REFERENCES `siembras` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 42. Ventas y cuentas por cobrar
CREATE TABLE IF NOT EXISTS `ventas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agricultor_id` bigint NOT NULL,
  -- Siembra de donde salió lo vendido (opcional)
  `siembra_id` bigint DEFAULT NULL,
  `comprador` varchar(120) NOT NULL,
  `fecha` date NOT NULL,
  `producto` varchar(120) NOT NULL,
  `cantidad_kg` decimal(12,2) DEFAULT NULL,
  `precio_kg` decimal(14,2) DEFAULT NULL,
  `total` decimal(14,2) NOT NULL,
  -- Fecha en que el comprador quedó de pagar (null: de contado o sin fecha)
  `vence` date DEFAULT NULL,
  `observaciones` varchar(255) DEFAULT NULL,
  `creado_en` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_venta_agricultor_fecha` (`agricultor_id`, `fecha`),
  KEY `idx_venta_siembra` (`siembra_id`),
  CONSTRAINT `fk_venta_agricultor` FOREIGN KEY (`agricultor_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_venta_siembra` FOREIGN KEY (`siembra_id`) REFERENCES `siembras` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `abonos_venta` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `venta_id` bigint NOT NULL,
  `fecha` date NOT NULL,
  `monto` decimal(14,2) NOT NULL,
  -- EFECTIVO, TRANSFERENCIA, CHEQUE
  `metodo` varchar(20) NOT NULL,
  `observaciones` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_abono_venta` (`venta_id`),
  CONSTRAINT `fk_abono_venta` FOREIGN KEY (`venta_id`) REFERENCES `ventas` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 43. Presupuesto por temporada (por las mismas partes en que se cuentan los costos)
CREATE TABLE IF NOT EXISTS `presupuestos_siembra` (
  `siembra_id` bigint NOT NULL,
  `mano_obra` decimal(14,2) DEFAULT NULL,
  `insumos` decimal(14,2) DEFAULT NULL,
  `otros` decimal(14,2) DEFAULT NULL,
  -- Hasta qué porcentaje ya se avisó (90 o 100) para no repetir el aviso cada día
  `avisado` int NOT NULL DEFAULT 0,
  `actualizado_en` datetime(6) NOT NULL,
  PRIMARY KEY (`siembra_id`),
  CONSTRAINT `fk_presupuesto_siembra` FOREIGN KEY (`siembra_id`) REFERENCES `siembras` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
