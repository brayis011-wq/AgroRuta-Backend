-- Integridad, parte 1: quitar los registros sueltos y crear los índices que faltaban.
-- Deja la base lista para las claves foráneas de V15.
--
-- Registros sueltos: filas que apuntan a algo que ya no existe (una finca, un lote o una siembra
-- que se borraron antes del borrado lógico de V6, o datos de pruebas). La aplicación no las muestra
-- en ninguna pantalla del agricultor porque no se puede saber de quién son.
--   - Si el dato que falta es obligatorio, la fila se borra (de arriba hacia abajo: finca, lote,
--     siembra y lo que cuelga de la siembra).
--   - Si es opcional, solo se deja vacío.
-- Los jornales NO se tocan: guardan el nombre del cultivo y son historial de nómina aunque la
-- siembra ya no exista. Por eso jornales.cultivo_id tampoco lleva clave foránea en V15.
--
-- Compatible con MySQL 8 y TiDB (un cambio por sentencia).

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. Cultivos: finca -> lote -> siembra -> etapas, actividades, cosechas, fumigaciones, diagnósticos
-- ─────────────────────────────────────────────────────────────────────────────
DELETE f FROM fincas f LEFT JOIN usuarios u ON u.id = f.agricultor_id WHERE u.id IS NULL;
DELETE l FROM lotes l LEFT JOIN fincas f ON f.id = l.finca_id WHERE f.id IS NULL;
DELETE s FROM siembras s LEFT JOIN lotes l ON l.id = s.lote_id WHERE l.id IS NULL;

UPDATE siembras s LEFT JOIN variedades_cultivo v ON v.id = s.variedad_id
SET s.variedad_id = NULL
WHERE s.variedad_id IS NOT NULL AND v.id IS NULL;

DELETE e FROM siembra_etapas e LEFT JOIN siembras s ON s.id = e.siembra_id WHERE s.id IS NULL;
DELETE a FROM actividades_cultivo a LEFT JOIN siembras s ON s.id = a.siembra_id WHERE s.id IS NULL;
DELETE c FROM cosechas c LEFT JOIN siembras s ON s.id = c.siembra_id WHERE s.id IS NULL;
DELETE f FROM fumigaciones f LEFT JOIN siembras s ON s.id = f.siembra_id WHERE s.id IS NULL;
DELETE d FROM diagnosticos_plaga d LEFT JOIN siembras s ON s.id = d.siembra_id WHERE s.id IS NULL;
DELETE d FROM diagnosticos_plaga d LEFT JOIN usuarios u ON u.id = d.usuario_id WHERE u.id IS NULL;

UPDATE fumigaciones f LEFT JOIN agricultural_inputs i ON i.id = f.agricultural_input_id
SET f.agricultural_input_id = NULL
WHERE f.agricultural_input_id IS NOT NULL AND i.id IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. Bodega de insumos
-- ─────────────────────────────────────────────────────────────────────────────
DELETE m FROM movimientos_inventario m LEFT JOIN usuarios u ON u.id = m.agricultor_id WHERE u.id IS NULL;
DELETE m FROM movimientos_inventario m LEFT JOIN agricultural_inputs i ON i.id = m.agricultural_input_id WHERE i.id IS NULL;
-- Gasto de una fumigación que ya no existe: el producto vuelve a la bodega, igual que al borrarla desde la app
DELETE m FROM movimientos_inventario m LEFT JOIN fumigaciones f ON f.id = m.fumigacion_id
WHERE m.fumigacion_id IS NOT NULL AND f.id IS NULL;

DELETE k FROM stock_minimo_insumo k LEFT JOIN usuarios u ON u.id = k.agricultor_id WHERE u.id IS NULL;
DELETE k FROM stock_minimo_insumo k LEFT JOIN agricultural_inputs i ON i.id = k.agricultural_input_id WHERE i.id IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. Trabajadores: el agricultor y la cuenta del portal son opcionales, solo se dejan vacíos
-- ─────────────────────────────────────────────────────────────────────────────
UPDATE cargos c LEFT JOIN usuarios u ON u.id = c.agricultor_id
SET c.agricultor_id = NULL
WHERE c.agricultor_id IS NOT NULL AND u.id IS NULL;

UPDATE trabajadores t LEFT JOIN usuarios u ON u.id = t.agricultor_id
SET t.agricultor_id = NULL
WHERE t.agricultor_id IS NOT NULL AND u.id IS NULL;

UPDATE trabajadores t LEFT JOIN usuarios u ON u.id = t.usuario_id
SET t.usuario_id = NULL
WHERE t.usuario_id IS NOT NULL AND u.id IS NULL;

UPDATE actividades a LEFT JOIN usuarios u ON u.id = a.agricultor_id
SET a.agricultor_id = NULL
WHERE a.agricultor_id IS NOT NULL AND u.id IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. Cuentas: sesiones, enlaces de correo, preferencias, segundo factor y avisos push
-- ─────────────────────────────────────────────────────────────────────────────
DELETE x FROM sesiones x LEFT JOIN usuarios u ON u.id = x.usuario_id WHERE u.id IS NULL;
DELETE x FROM restablecimientos_contrasena x LEFT JOIN usuarios u ON u.id = x.usuario_id WHERE u.id IS NULL;
DELETE x FROM verificaciones_correo x LEFT JOIN usuarios u ON u.id = x.usuario_id WHERE u.id IS NULL;
DELETE x FROM preferencias_correo x LEFT JOIN usuarios u ON u.id = x.usuario_id WHERE u.id IS NULL;
DELETE x FROM segundo_factor x LEFT JOIN usuarios u ON u.id = x.usuario_id WHERE u.id IS NULL;
DELETE x FROM suscripciones_push x LEFT JOIN usuarios u ON u.id = x.usuario_id WHERE u.id IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. Índices que faltaban. Casi todo se busca por siembra o por agricultor, y esas columnas
--    no tenían índice (o eran la última de un índice único, que no sirve para buscar por ellas).
-- ─────────────────────────────────────────────────────────────────────────────
CREATE INDEX idx_siembra_variedad ON siembras (variedad_id);
CREATE INDEX idx_etapa_siembra ON siembra_etapas (siembra_id, fecha);
CREATE INDEX idx_actividad_cultivo_siembra ON actividades_cultivo (siembra_id, fecha);
CREATE INDEX idx_cosecha_siembra ON cosechas (siembra_id, fecha);
CREATE INDEX idx_fumigacion_siembra ON fumigaciones (siembra_id, fecha);
CREATE INDEX idx_fumigacion_insumo ON fumigaciones (agricultural_input_id);
CREATE INDEX idx_diagnostico_usuario ON diagnosticos_plaga (usuario_id);

CREATE INDEX idx_mov_insumo ON movimientos_inventario (agricultural_input_id);
CREATE INDEX idx_minimo_insumo ON stock_minimo_insumo (agricultural_input_id);

CREATE INDEX idx_cargo_agricultor ON cargos (agricultor_id);
CREATE INDEX idx_trabajador_agricultor ON trabajadores (agricultor_id);
CREATE INDEX idx_trabajador_usuario ON trabajadores (usuario_id);
CREATE INDEX idx_labor_agricultor ON actividades (agricultor_id);
CREATE INDEX idx_jornal_cultivo ON jornales (cultivo_id, fecha);
