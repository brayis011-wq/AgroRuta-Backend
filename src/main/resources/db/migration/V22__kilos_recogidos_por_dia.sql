-- ─────────────────────────────────────────────────────────────────────────────
-- V22: kilos que recogió un recolector pagado por día.
--
-- En el jornal, "kilos" son los que se pagan (pago al kilo). Quien cobra por día puede
-- anotar además cuántos kilos recogió: no cambia el pago, pero arma el total de la
-- cosecha y deja ver el rendimiento de cada trabajador (kilos por día y cuánto sale el
-- kilo pagando por día o por kilo).
--
-- Solo agrega una columna que acepta NULL: la versión anterior del backend sigue
-- funcionando sobre este esquema mientras se despliega la nueva. Compatible con MySQL 8 y TiDB.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE jornales ADD COLUMN kilos_recogidos decimal(10,2) DEFAULT NULL;
