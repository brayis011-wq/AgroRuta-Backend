-- Integridad, parte 2: claves foráneas.
--
-- Hasta ahora solo las tenía el módulo de trabajadores (las creó Hibernate al principio).
-- Sin ellas, la base aceptaba una fumigación de una siembra que no existe o un lote sin finca.
-- V14 ya quitó los registros sueltos y creó los índices que estas claves necesitan.
--
-- Dos reglas:
--   - Datos del negocio (fincas, lotes, siembras, cosechas, bodega...): sin ON DELETE. La base
--     no deja borrar un padre que todavía tiene hijos; la aplicación usa borrado lógico.
--   - Datos que solo existen por la cuenta (sesiones, enlaces de correo, preferencias, segundo
--     factor, avisos push): ON DELETE CASCADE. Si algún día se borra un usuario, se van con él.
--
-- Sin clave foránea a propósito:
--   - jornales.cultivo_id: el jornal es historial de nómina y guarda el nombre del cultivo;
--     debe seguir existiendo aunque la siembra se haya borrado hace tiempo.
--   - envios_recibidos y diagnosticos_uso: contadores técnicos que se borran solos.
--
-- Compatible con MySQL 8 y TiDB 6.6 o superior (un cambio por sentencia). En un TiDB más viejo
-- las sentencias se aceptan pero no tienen efecto.

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. Cultivos
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE fincas ADD CONSTRAINT fk_finca_agricultor FOREIGN KEY (agricultor_id) REFERENCES usuarios (id);
ALTER TABLE lotes ADD CONSTRAINT fk_lote_finca FOREIGN KEY (finca_id) REFERENCES fincas (id);
ALTER TABLE siembras ADD CONSTRAINT fk_siembra_lote FOREIGN KEY (lote_id) REFERENCES lotes (id);
ALTER TABLE siembras ADD CONSTRAINT fk_siembra_variedad FOREIGN KEY (variedad_id) REFERENCES variedades_cultivo (id);
ALTER TABLE siembra_etapas ADD CONSTRAINT fk_etapa_siembra FOREIGN KEY (siembra_id) REFERENCES siembras (id);
ALTER TABLE actividades_cultivo ADD CONSTRAINT fk_actividad_cultivo_siembra FOREIGN KEY (siembra_id) REFERENCES siembras (id);
ALTER TABLE cosechas ADD CONSTRAINT fk_cosecha_siembra FOREIGN KEY (siembra_id) REFERENCES siembras (id);
ALTER TABLE fumigaciones ADD CONSTRAINT fk_fumigacion_siembra FOREIGN KEY (siembra_id) REFERENCES siembras (id);
ALTER TABLE fumigaciones ADD CONSTRAINT fk_fumigacion_insumo FOREIGN KEY (agricultural_input_id) REFERENCES agricultural_inputs (id);
ALTER TABLE diagnosticos_plaga ADD CONSTRAINT fk_diagnostico_siembra FOREIGN KEY (siembra_id) REFERENCES siembras (id);
ALTER TABLE diagnosticos_plaga ADD CONSTRAINT fk_diagnostico_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. Bodega de insumos
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE movimientos_inventario ADD CONSTRAINT fk_mov_agricultor FOREIGN KEY (agricultor_id) REFERENCES usuarios (id);
ALTER TABLE movimientos_inventario ADD CONSTRAINT fk_mov_insumo FOREIGN KEY (agricultural_input_id) REFERENCES agricultural_inputs (id);
ALTER TABLE movimientos_inventario ADD CONSTRAINT fk_mov_fumigacion FOREIGN KEY (fumigacion_id) REFERENCES fumigaciones (id);
ALTER TABLE stock_minimo_insumo ADD CONSTRAINT fk_minimo_agricultor FOREIGN KEY (agricultor_id) REFERENCES usuarios (id);
ALTER TABLE stock_minimo_insumo ADD CONSTRAINT fk_minimo_insumo FOREIGN KEY (agricultural_input_id) REFERENCES agricultural_inputs (id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. Trabajadores
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE cargos ADD CONSTRAINT fk_cargo_agricultor FOREIGN KEY (agricultor_id) REFERENCES usuarios (id);
ALTER TABLE trabajadores ADD CONSTRAINT fk_trabajador_agricultor FOREIGN KEY (agricultor_id) REFERENCES usuarios (id);
ALTER TABLE trabajadores ADD CONSTRAINT fk_trabajador_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id);
ALTER TABLE actividades ADD CONSTRAINT fk_labor_agricultor FOREIGN KEY (agricultor_id) REFERENCES usuarios (id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. Cuentas (se borran con el usuario)
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE sesiones ADD CONSTRAINT fk_sesion_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE;
ALTER TABLE restablecimientos_contrasena ADD CONSTRAINT fk_restablecimiento_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE;
ALTER TABLE verificaciones_correo ADD CONSTRAINT fk_verificacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE;
ALTER TABLE preferencias_correo ADD CONSTRAINT fk_preferencia_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE;
ALTER TABLE segundo_factor ADD CONSTRAINT fk_segundo_factor_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE;
ALTER TABLE suscripciones_push ADD CONSTRAINT fk_suscripcion_push_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE;
