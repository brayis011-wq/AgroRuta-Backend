-- Borrado lógico de fincas, lotes y siembras.
--
-- Antes, eliminar una siembra borraba sus actividades, fumigaciones y cosechas (y con ellas
-- sus gastos e ingresos), y eliminar un lote o una finca dejaba sus siembras fuera de los
-- reportes. Ahora se marca la fecha en eliminado_en: deja de verse en las pantallas, pero
-- todo el historial sigue en los reportes de cultivos y de costos.
--
-- Compatible con MySQL 8 y TiDB (una sola modificación por ALTER).

ALTER TABLE fincas   ADD COLUMN eliminado_en datetime(6) DEFAULT NULL;
ALTER TABLE lotes    ADD COLUMN eliminado_en datetime(6) DEFAULT NULL;
ALTER TABLE siembras ADD COLUMN eliminado_en datetime(6) DEFAULT NULL;

-- El nombre deja de ser único en la base: una finca o lote eliminado conserva su nombre y se
-- puede crear otro igual. La aplicación sigue sin permitir dos vigentes con el mismo nombre.
-- Se reemplaza por un índice normal que además sirve para listar por agricultor o por finca.
CREATE INDEX idx_finca_agricultor ON fincas (agricultor_id, nombre);
ALTER TABLE fincas DROP INDEX uk_finca_nombre_agricultor;

CREATE INDEX idx_lote_finca ON lotes (finca_id, nombre);
ALTER TABLE lotes DROP INDEX uk_lote_nombre_finca;

-- Para buscar las siembras de un lote sin recorrer toda la tabla
CREATE INDEX idx_siembra_lote ON siembras (lote_id);
