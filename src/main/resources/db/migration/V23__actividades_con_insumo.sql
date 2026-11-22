-- ─────────────────────────────────────────────────────────────────────────────
-- V23: actividades que gastan un insumo de la bodega (abonado, fertilización al suelo...).
--
-- Igual que las fumigaciones: la actividad puede decir qué producto se usó y cuánto en
-- total. Ese gasto se descuenta de la bodega (un movimiento USO con actividad_id) y, si no
-- se escribe el costo, se calcula a precio promedio de compra.
--
-- Nuevo tipo ABONADO (va al final de la lista: agregar un valor al final de un enum no
-- reescribe la tabla en MySQL ni en TiDB). Las columnas nuevas aceptan NULL: la versión
-- anterior del backend sigue funcionando sobre este esquema mientras se despliega la nueva.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE actividades_cultivo
    MODIFY COLUMN tipo enum('DESHIERBE','OTRO','PODA','REVISION','RIEGO','TUTOREO','ABONADO') NOT NULL;

ALTER TABLE actividades_cultivo ADD COLUMN producto varchar(150) DEFAULT NULL;
ALTER TABLE actividades_cultivo ADD COLUMN agricultural_input_id bigint DEFAULT NULL;
ALTER TABLE actividades_cultivo ADD COLUMN cantidad_usada double DEFAULT NULL;
ALTER TABLE actividades_cultivo ADD COLUMN unidad_cantidad varchar(10) DEFAULT NULL;

ALTER TABLE movimientos_inventario ADD COLUMN actividad_id bigint DEFAULT NULL;
CREATE UNIQUE INDEX uk_movimiento_actividad ON movimientos_inventario (actividad_id);
