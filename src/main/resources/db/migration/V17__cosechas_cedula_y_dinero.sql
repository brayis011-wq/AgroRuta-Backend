-- ─────────────────────────────────────────────────────────────────────────────
-- V17: reglas de negocio que limitaban el uso real, y dinero en decimal.
--
-- 1. Cédula única por agricultor (antes: única en toda la base). Un jornalero puede
--    trabajar para dos agricultores; cada uno lo registra en su propia lista.
-- 2. Una cosecha por día, siembra Y calidad (antes: una por día y siembra). El mismo día
--    se puede registrar primera, segunda y tercera.
-- 3. Costos y precios en decimal(14,2), como ya están nóminas y pagos (antes: double).
--
-- Las reglas nuevas son más amplias que las viejas, así que ningún dato existente las
-- incumple. La versión anterior del backend sigue funcionando sobre este esquema mientras
-- se despliega la nueva: sus validaciones son más estrictas y lee decimal sin problema.
--
-- Compatible con MySQL 8 y TiDB. Cada índice nuevo se crea antes de borrar el viejo.
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. Cédula
ALTER TABLE trabajadores ADD UNIQUE KEY uk_trabajador_cedula_agricultor (cedula, agricultor_id);
ALTER TABLE trabajadores DROP INDEX UK5yn53m1kktaocss6wwvbvksew;

-- 2. Cosechas (la clave foránea a siembras se apoya en idx_cosecha_siembra, de V14)
ALTER TABLE cosechas ADD UNIQUE KEY uk_cosecha_fecha_siembra_calidad (fecha, siembra_id, calidad);
ALTER TABLE cosechas DROP INDEX uk_cosecha_fecha_siembra;

-- 3. Dinero
ALTER TABLE actividades_cultivo MODIFY COLUMN costo decimal(14,2) DEFAULT NULL;
ALTER TABLE cosechas MODIFY COLUMN precio_kg decimal(14,2) DEFAULT NULL;
ALTER TABLE fumigaciones MODIFY COLUMN costo decimal(14,2) DEFAULT NULL;
ALTER TABLE movimientos_inventario MODIFY COLUMN costo_total decimal(14,2) DEFAULT NULL;
