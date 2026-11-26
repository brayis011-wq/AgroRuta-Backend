-- Cosechas registradas dentro del periodo de carencia de una fumigación.
--
-- El backend ya no deja registrar una cosecha antes de que pasen los días de carencia del último
-- producto aplicado, salvo que la persona lo confirme. Esas cosechas quedan marcadas aquí para
-- que se vean en la bitácora y en los reportes.
--
-- Compatible con MySQL 8 y TiDB.

ALTER TABLE `cosechas` ADD COLUMN `en_carencia` bit(1) NOT NULL DEFAULT b'0';
