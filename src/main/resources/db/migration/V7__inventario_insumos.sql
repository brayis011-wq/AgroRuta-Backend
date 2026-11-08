-- Inventario de insumos: una bodega por agricultor.
-- El stock no se guarda: se calcula sumando los movimientos (compras, gastos en fumigaciones
-- y ajustes por conteo). Las cantidades van en la unidad base del insumo: ml o g.
--
-- Compatible con MySQL 8 y TiDB.

CREATE TABLE IF NOT EXISTS `movimientos_inventario` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agricultor_id` bigint NOT NULL,
  `agricultural_input_id` bigint NOT NULL,
  `tipo` enum('AJUSTE','COMPRA','USO') NOT NULL,
  `cantidad` double NOT NULL,
  `fecha` date NOT NULL,
  `costo_total` double DEFAULT NULL,
  `proveedor` varchar(150) DEFAULT NULL,
  `fumigacion_id` bigint DEFAULT NULL,
  `observaciones` varchar(500) DEFAULT NULL,
  `creado_en` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_mov_agricultor_insumo` (`agricultor_id`, `agricultural_input_id`),
  UNIQUE KEY `uk_mov_fumigacion` (`fumigacion_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `stock_minimo_insumo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agricultor_id` bigint NOT NULL,
  `agricultural_input_id` bigint NOT NULL,
  `minimo` double NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_minimo_agricultor_insumo` (`agricultor_id`, `agricultural_input_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Cantidad total de producto que se gastó en la fumigación (la "dosis" es una concentración,
-- p. ej. 500 g por 200 L de agua). Es lo que se descuenta de la bodega.
ALTER TABLE `fumigaciones` ADD COLUMN `cantidad_usada` double DEFAULT NULL;
ALTER TABLE `fumigaciones` ADD COLUMN `unidad_cantidad` enum('GRAMOS','KG','LITROS','ML') DEFAULT NULL;
