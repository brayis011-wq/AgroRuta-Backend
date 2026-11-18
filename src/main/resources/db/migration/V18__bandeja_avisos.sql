-- Bandeja de avisos dentro de la app (la campana): lo que antes solo llegaba por correo o push
-- queda guardado para volver a verlo. Se guarda aunque la persona no tenga avisos push activos.
CREATE TABLE IF NOT EXISTS `avisos_bandeja` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint NOT NULL,
  -- Qué pasó (PAGO_REGISTRADO, ACCESO_SOLICITADO, ACCESO_RESUELTO, RESUMEN_DIA): el frontend arma el texto en el idioma de la persona
  `tipo` varchar(40) NOT NULL,
  -- Texto en español por si el frontend no conoce el tipo
  `titulo` varchar(120) NOT NULL,
  `cuerpo` varchar(400) NOT NULL,
  -- Datos para armar el texto (JSON pequeño: monto, nombre del trabajador, conteos del día)
  `datos` varchar(1000) DEFAULT NULL,
  -- Pantalla que se abre al tocar el aviso (/portal/payments, /worker...)
  `ruta` varchar(200) DEFAULT NULL,
  -- El mismo hecho no se repite: un aviso con la misma etiqueta se actualiza y vuelve a quedar sin leer
  `etiqueta` varchar(80) NOT NULL,
  `creado_en` datetime(6) NOT NULL,
  `leido_en` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_aviso_bandeja_etiqueta` (`usuario_id`, `etiqueta`),
  KEY `idx_aviso_bandeja_usuario_fecha` (`usuario_id`, `creado_en`),
  CONSTRAINT `fk_aviso_bandeja_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
