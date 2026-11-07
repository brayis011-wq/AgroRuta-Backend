-- Datos que antes cargaban o corregían, en CADA arranque, CatalogoCultivoInicializador
-- y TemporadasInicializador. Ahora corren una sola vez y quedan en el historial de Flyway.
-- Todo es idempotente: en producción (donde los inicializadores ya corrieron) no cambia nada.
--
-- Los cambios de esquema que también hacían (siembras.variedad de ENUM a texto, quitar el
-- índice único de siembras.lote_id, usuarios.foto_perfil a MEDIUMTEXT) no se repiten aquí:
-- V1 ya crea las tablas así.
--
-- Compatible con MySQL 8 y TiDB (sin procedimientos ni CREATE TABLE ... SELECT).

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. Catálogo inicial de cultivos: solo en una base nueva (sin ningún tipo de cultivo).
--    Si el administrador ya editó el catálogo, no se toca.
-- ─────────────────────────────────────────────────────────────────────────────
SET @catalogo_vacio = (SELECT COUNT(*) FROM tipos_cultivo) = 0;

INSERT INTO tipos_cultivo (nombre, activo)
SELECT c.nombre, b'1'
FROM (
    SELECT 'Uchuva' AS nombre UNION ALL SELECT 'Café' UNION ALL SELECT 'Aguacate'
    UNION ALL SELECT 'Papa' UNION ALL SELECT 'Tomate' UNION ALL SELECT 'Mora'
    UNION ALL SELECT 'Fresa' UNION ALL SELECT 'Lulo' UNION ALL SELECT 'Plátano'
    UNION ALL SELECT 'Cacao' UNION ALL SELECT 'Maíz' UNION ALL SELECT 'Fríjol'
) c
WHERE @catalogo_vacio = 1;

INSERT INTO variedades_cultivo (nombre, activo, tipo_cultivo_id)
SELECT v.variedad, b'1', t.id
FROM (
              SELECT 'Uchuva' AS cultivo, 'Colombia' AS variedad
    UNION ALL SELECT 'Uchuva', 'Gigante'
    UNION ALL SELECT 'Uchuva', 'Kenya'
    UNION ALL SELECT 'Café', 'Castillo'
    UNION ALL SELECT 'Café', 'Caturra'
    UNION ALL SELECT 'Café', 'Colombia'
    UNION ALL SELECT 'Café', 'Cenicafé 1'
    UNION ALL SELECT 'Aguacate', 'Hass'
    UNION ALL SELECT 'Aguacate', 'Lorena'
    UNION ALL SELECT 'Aguacate', 'Choquette'
    UNION ALL SELECT 'Papa', 'Pastusa Suprema'
    UNION ALL SELECT 'Papa', 'Criolla'
    UNION ALL SELECT 'Papa', 'Diacol Capiro'
    UNION ALL SELECT 'Tomate', 'Chonto'
    UNION ALL SELECT 'Tomate', 'Larga vida'
    UNION ALL SELECT 'Mora', 'Castilla'
    UNION ALL SELECT 'Mora', 'Sin espinas'
    UNION ALL SELECT 'Fresa', 'Albión'
    UNION ALL SELECT 'Fresa', 'Monterrey'
    UNION ALL SELECT 'Lulo', 'Castilla'
    UNION ALL SELECT 'Lulo', 'La Selva'
    UNION ALL SELECT 'Plátano', 'Hartón'
    UNION ALL SELECT 'Plátano', 'Dominico'
    UNION ALL SELECT 'Cacao', 'CCN-51'
    UNION ALL SELECT 'Cacao', 'ICS-95'
    UNION ALL SELECT 'Maíz', 'Amarillo'
    UNION ALL SELECT 'Maíz', 'Blanco'
    UNION ALL SELECT 'Fríjol', 'Cargamanto'
    UNION ALL SELECT 'Fríjol', 'Bola roja'
) v
JOIN tipos_cultivo t ON t.nombre = v.cultivo
WHERE @catalogo_vacio = 1;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. Siembras de antes del catálogo (solo había uchuva, con la variedad en mayúsculas).
--    COLLATE utf8mb4_bin: compara distinguiendo mayúsculas, para no "actualizar"
--    'Colombia' por 'Colombia' (en MySQL la comparación normal no las distingue).
-- ─────────────────────────────────────────────────────────────────────────────
UPDATE siembras SET variedad = 'Colombia' WHERE variedad COLLATE utf8mb4_bin = 'COLOMBIA';
UPDATE siembras SET variedad = 'Gigante'  WHERE variedad COLLATE utf8mb4_bin = 'GIGANTE';
UPDATE siembras SET variedad = 'Kenya'    WHERE variedad COLLATE utf8mb4_bin = 'KENYA';

UPDATE siembras SET tipo_cultivo = 'Uchuva' WHERE tipo_cultivo IS NULL;

UPDATE siembras s
JOIN variedades_cultivo v ON v.nombre = s.variedad
JOIN tipos_cultivo t ON t.id = v.tipo_cultivo_id AND t.nombre = s.tipo_cultivo
SET s.variedad_id = v.id
WHERE s.variedad_id IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. Temporadas por lote: el estado del lote sigue a su siembra en curso.
-- ─────────────────────────────────────────────────────────────────────────────
UPDATE lotes l SET estado = 'EN_CULTIVO'
WHERE l.estado <> 'EN_CULTIVO'
  AND EXISTS (SELECT 1 FROM siembras s WHERE s.lote_id = l.id AND s.estado_cultivo <> 'FINALIZADO');

UPDATE lotes l SET estado = 'DISPONIBLE'
WHERE l.estado = 'EN_CULTIVO'
  AND NOT EXISTS (SELECT 1 FROM siembras s WHERE s.lote_id = l.id AND s.estado_cultivo <> 'FINALIZADO');

-- 4. Siembras antiguas sin historial de etapas: empiezan en germinación en su fecha de siembra.
INSERT INTO siembra_etapas (siembra_id, etapa, fecha)
SELECT s.id, 'GERMINACION', s.fecha_siembra
FROM siembras s
WHERE NOT EXISTS (SELECT 1 FROM siembra_etapas e WHERE e.siembra_id = s.id);
