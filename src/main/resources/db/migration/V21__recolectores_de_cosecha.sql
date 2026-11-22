-- ─────────────────────────────────────────────────────────────────────────────
-- V21: quién recogió cada cosecha.
--
-- Al registrar una cosecha se anota a los recolectores (por kilo o por día) y a cada uno
-- se le crea su jornal de ese día; el jornal guarda de qué cosecha salió para poder
-- corregirlo o quitarlo junto con ella. Los jornales que no vienen de una cosecha
-- quedan con cosecha_id en NULL.
--
-- Sin llave foránea (como el resto de columnas agregadas a jornales): al borrar una
-- cosecha, el backend borra sus jornales sin nómina y desliga los que ya están en una.
--
-- Solo agrega una columna que acepta NULL: la versión anterior del backend sigue
-- funcionando sobre este esquema mientras se despliega la nueva. Compatible con MySQL 8 y TiDB.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE jornales ADD COLUMN cosecha_id bigint DEFAULT NULL;
CREATE INDEX idx_jornal_cosecha ON jornales (cosecha_id);
