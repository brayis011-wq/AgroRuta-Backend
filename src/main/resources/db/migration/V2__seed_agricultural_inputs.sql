INSERT IGNORE INTO agricultural_inputs (nombre, tipo, unidad_sugerida, dosis_sugerida, reentrada_horas, activo, creado_en)
VALUES
-- FUNGICIDAS
('Clorothalonil 72%',               'FUNGICIDA',              'GRAMOS', 150,  24,  true, NOW()),
('Mancozeb 80%',                    'FUNGICIDA',              'GRAMOS', 200,  24,  true, NOW()),
('Metalaxil + Mancozeb',            'FUNGICIDA',              'GRAMOS', 250,  48,  true, NOW()),
('Azoxystrobin 25%',                'FUNGICIDA',              'ML',     100,  12,  true, NOW()),
('Tebuconazol 25%',                 'FUNGICIDA',              'ML',     75,   24,  true, NOW()),
('Oxicloruro de cobre 58.8%',       'FUNGICIDA',              'GRAMOS', 250,  12,  true, NOW()),
('Fosetil aluminio 80%',            'FUNGICIDA',              'GRAMOS', 200,  12,  true, NOW()),
('Captan 50%',                      'FUNGICIDA',              'GRAMOS', 200,  24,  true, NOW()),
('Propiconazol 25%',                'FUNGICIDA',              'ML',     75,   24,  true, NOW()),
('Iprodione 50%',                   'FUNGICIDA',              'GRAMOS', 150,  12,  true, NOW()),
('Trifloxystrobin + Tebuconazol',   'FUNGICIDA',              'ML',     75,   12,  true, NOW()),
('Cymoxanil + Mancozeb',            'FUNGICIDA',              'GRAMOS', 250,  24,  true, NOW()),
('Dimethomorph + Mancozeb',         'FUNGICIDA',              'GRAMOS', 200,  24,  true, NOW()),
('Fluopicolide + Propamocarb',      'FUNGICIDA',              'ML',     60,   12,  true, NOW()),
('Boscalid + Pyraclostrobin',       'FUNGICIDA',              'ML',     75,   12,  true, NOW()),
('Kasugamicina 2%',                 'FUNGICIDA',              'ML',     100,  4,   true, NOW()),
('Hidróxido de cobre 77%',          'FUNGICIDA',              'GRAMOS', 200,  12,  true, NOW()),
('Sulfato de cobre pentahidratado', 'FUNGICIDA',              'GRAMOS', 300,  24,  true, NOW()),

-- INSECTICIDAS
('Imidacloprid 35%',                'INSECTICIDA',            'ML',     50,   48,  true, NOW()),
('Abamectina 1.8%',                 'INSECTICIDA',            'ML',     60,   72,  true, NOW()),
('Spinosad 48%',                    'INSECTICIDA',            'ML',     40,   4,   true, NOW()),
('Clorpirifos 48%',                 'INSECTICIDA',            'ML',     100,  48,  true, NOW()),
('Lambda-cihalotrina 5%',           'INSECTICIDA',            'ML',     30,   24,  true, NOW()),
('Thiamethoxam 25%',                'INSECTICIDA',            'GRAMOS', 40,   12,  true, NOW()),
('Acetamiprid 20%',                 'INSECTICIDA',            'GRAMOS', 25,   24,  true, NOW()),
('Bifentrina 10%',                  'INSECTICIDA',            'ML',     50,   12,  true, NOW()),
('Metomilo 90%',                    'INSECTICIDA',            'GRAMOS', 75,   48,  true, NOW()),
('Cipermetrina 20%',                'INSECTICIDA',            'ML',     50,   24,  true, NOW()),
('Deltametrina 1%',                 'INSECTICIDA',            'ML',     30,   12,  true, NOW()),
('Dimetoato 40%',                   'INSECTICIDA',            'ML',     75,   48,  true, NOW()),
('Spirotetramat 15%',               'INSECTICIDA',            'ML',     80,   8,   true, NOW()),
('Beauveria bassiana',              'INSECTICIDA',            'GRAMOS', 200,  0,   true, NOW()),
('Metarhizium anisopliae',          'INSECTICIDA',            'GRAMOS', 200,  0,   true, NOW()),
('Bacillus thuringiensis',          'INSECTICIDA',            'GRAMOS', 150,  0,   true, NOW()),

-- ACARICIDAS
('Abamectina + Bifentrina',         'ACARICIDA',              'ML',     60,   48,  true, NOW()),
('Hexitiazox 10%',                  'ACARICIDA',              'ML',     50,   24,  true, NOW()),
('Fenpiroximato 5%',                'ACARICIDA',              'ML',     75,   24,  true, NOW()),
('Etoxazol 11%',                    'ACARICIDA',              'ML',     40,   24,  true, NOW()),
('Clofentezine 50%',                'ACARICIDA',              'ML',     50,   12,  true, NOW()),
('Azufre mojable 80%',              'ACARICIDA',              'GRAMOS', 300,  24,  true, NOW()),

-- NEMATICIDAS
('Cadusafos 10%',                   'NEMATICIDA',             'GRAMOS', 30,   120, true, NOW()),
('Oxamilo 24%',                     'NEMATICIDA',             'ML',     100,  72,  true, NOW()),
('Fluensulfone 40%',                'NEMATICIDA',             'ML',     50,   24,  true, NOW()),
('Paecilomyces lilacinus',          'NEMATICIDA',             'GRAMOS', 200,  0,   true, NOW()),
('Bacillus firmus',                 'NEMATICIDA',             'GRAMOS', 150,  0,   true, NOW()),

-- HERBICIDAS
('Glifosato 48%',                   'HERBICIDA',              'ML',     200,  12,  true, NOW()),
('Paraquat 27.6%',                  'HERBICIDA',              'ML',     150,  24,  true, NOW()),
('Atrazina 80%',                    'HERBICIDA',              'GRAMOS', 1500, 12,  true, NOW()),
('Metribuzin 70%',                  'HERBICIDA',              'GRAMOS', 350,  12,  true, NOW()),
('Pendimetalina 33%',               'HERBICIDA',              'ML',     300,  12,  true, NOW()),
('Oxifluorfen 24%',                 'HERBICIDA',              'ML',     200,  24,  true, NOW()),
('2,4-D amina 72%',                 'HERBICIDA',              'ML',     150,  24,  true, NOW()),
('Haloxifop-R metil 10.8%',         'HERBICIDA',              'ML',     75,   24,  true, NOW()),
('Fluazifop-p-butil 12.5%',         'HERBICIDA',              'ML',     100,  12,  true, NOW()),
('Bentazon 48%',                    'HERBICIDA',              'ML',     200,  12,  true, NOW()),

-- FERTILIZANTES FOLIARES
('Nitrato de calcio',               'FERTILIZANTE_FOLIAR',    'GRAMOS', 300,  0,   true, NOW()),
('Boro foliar',                     'FERTILIZANTE_FOLIAR',    'ML',     100,  0,   true, NOW()),
('Nitrato de potasio',              'FERTILIZANTE_FOLIAR',    'GRAMOS', 250,  0,   true, NOW()),
('Sulfato de magnesio',             'FERTILIZANTE_FOLIAR',    'GRAMOS', 200,  0,   true, NOW()),
('Fosfato monoamónico foliar',      'FERTILIZANTE_FOLIAR',    'GRAMOS', 200,  0,   true, NOW()),
('Zinc EDTA 15%',                   'FERTILIZANTE_FOLIAR',    'ML',     100,  0,   true, NOW()),
('Hierro EDTA 13%',                 'FERTILIZANTE_FOLIAR',    'ML',     100,  0,   true, NOW()),
('Manganeso EDTA 13%',              'FERTILIZANTE_FOLIAR',    'ML',     75,   0,   true, NOW()),
('Molibdato de sodio',              'FERTILIZANTE_FOLIAR',    'GRAMOS', 10,   0,   true, NOW()),
('Aminoácidos + microelementos',    'FERTILIZANTE_FOLIAR',    'ML',     150,  0,   true, NOW()),
('Silicio soluble',                 'FERTILIZANTE_FOLIAR',    'ML',     100,  0,   true, NOW()),

-- FERTILIZANTES AL SUELO
('Urea 46%',                        'FERTILIZANTE_SUELO',     'GRAMOS', 500,  0,   true, NOW()),
('DAP Fosfato diamónico',           'FERTILIZANTE_SUELO',     'GRAMOS', 400,  0,   true, NOW()),
('KCl Cloruro de potasio',          'FERTILIZANTE_SUELO',     'GRAMOS', 300,  0,   true, NOW()),
('Fertilizante 10-30-10',           'FERTILIZANTE_SUELO',     'GRAMOS', 500,  0,   true, NOW()),
('Fertilizante 15-15-15',           'FERTILIZANTE_SUELO',     'GRAMOS', 500,  0,   true, NOW()),
('Fertilizante 13-26-6',            'FERTILIZANTE_SUELO',     'GRAMOS', 400,  0,   true, NOW()),
('Sulfato de amonio 21%',           'FERTILIZANTE_SUELO',     'GRAMOS', 400,  0,   true, NOW()),
('Cal dolomita',                    'FERTILIZANTE_SUELO',     'GRAMOS', 2000, 0,   true, NOW()),
('Yeso agrícola',                   'FERTILIZANTE_SUELO',     'GRAMOS', 1000, 0,   true, NOW()),
('Humus de lombriz',                'FERTILIZANTE_SUELO',     'GRAMOS', 3000, 0,   true, NOW()),

-- REGULADORES DE CRECIMIENTO
('Ácido giberélico 10%',            'REGULADOR_CRECIMIENTO',  'GRAMOS', 10,   0,   true, NOW()),
('Citocinina Kinetina',             'REGULADOR_CRECIMIENTO',  'ML',     50,   0,   true, NOW()),
('Ethephon 48%',                    'REGULADOR_CRECIMIENTO',  'ML',     75,   0,   true, NOW()),
('Paclobutrazol 25%',               'REGULADOR_CRECIMIENTO',  'ML',     50,   0,   true, NOW()),
('Trinexapac-etil 25%',             'REGULADOR_CRECIMIENTO',  'ML',     40,   0,   true, NOW()),

-- COADYUVANTES
('Aceite agrícola 96%',             'COADYUVANTE',            'ML',     500,  0,   true, NOW()),
('Siliconas spreader sticker',      'COADYUVANTE',            'ML',     25,   0,   true, NOW()),
('Sulfato de amonio coadyuvante',   'COADYUVANTE',            'GRAMOS', 500,  0,   true, NOW()),
('Regulador de pH',                 'COADYUVANTE',            'ML',     10,   0,   true, NOW());