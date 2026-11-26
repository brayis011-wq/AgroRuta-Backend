-- ─────────────────────────────────────────────────────────────────────────────
-- V20: labores comunes de una finca, para que una cuenta nueva no empiece con la lista vacía.
--
-- Son labores comunes (sin agricultor): todos las ven y solo el Administrador las cambia, igual
-- que "Fumigar". No se repite ninguna que ya exista con el mismo nombre (o el nombre parecido que
-- se indica en "parecida", p. ej. Fumigar ↔ Fumigación).
-- Compatible con MySQL 8 y TiDB (INSERT ... SELECT, sin procedimientos).
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO actividades (activa, nombre, descripcion, agricultor_id)
SELECT b'1', l.nombre, l.descripcion, NULL
FROM (
              SELECT 'Recolección' AS nombre, 'Cosechar o recoger el producto (café, fruta, plátano, papa)' AS descripcion, 'Cosecha' AS parecida
    UNION ALL SELECT 'Desyerba', 'Quitar maleza a mano o con azadón; plateo alrededor de las plantas', 'Deshierbe'
    UNION ALL SELECT 'Guadañada', 'Rocería de calles, linderos y potreros con guadaña', 'Rocería'
    UNION ALL SELECT 'Fumigación', 'Aplicar fungicidas, insecticidas o herbicidas con bomba', 'Fumigar'
    UNION ALL SELECT 'Fertilización', 'Abonar las plantas: al suelo o foliar', 'Abonada'
    UNION ALL SELECT 'Poda', 'Podar, deschuponar o desramar', 'Podar'
    UNION ALL SELECT 'Siembra y resiembra', 'Sembrar plántulas o reponer las que se perdieron', 'Siembra'
    UNION ALL SELECT 'Riego', 'Regar o mover mangueras y aspersores', 'Regar'
    UNION ALL SELECT 'Beneficio', 'Despulpar, lavar y secar el café, o lavar la cosecha', 'Beneficiadero'
    UNION ALL SELECT 'Selección y empaque', 'Clasificar por calidad y empacar para la venta', 'Empaque'
    UNION ALL SELECT 'Acarreo', 'Cargar y llevar insumos o cosecha dentro de la finca', 'Transporte'
    UNION ALL SELECT 'Mantenimiento', 'Arreglo de caminos, cercas, drenajes y zanjas', 'Mantenimiento de finca'
    UNION ALL SELECT 'Tutorado', 'Poner o ajustar tutores, amarres y espalderas', 'Tutoreo'
    UNION ALL SELECT 'Vivero', 'Germinador, almácigo y cuidado de plántulas', 'Almácigo'
    UNION ALL SELECT 'Deshoje y embolsado', 'Quitar hojas secas y embolsar racimos (plátano, banano)', 'Deshoje'
) l
WHERE NOT EXISTS (
    SELECT 1 FROM actividades a
     WHERE a.agricultor_id IS NULL AND a.activa = b'1'
       AND (LOWER(a.nombre) = LOWER(l.nombre) OR LOWER(a.nombre) = LOWER(l.parecida))
);
