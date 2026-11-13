-- =====================================================================
-- DATOS DE DEMOSTRACIÓN (Candelaria 2027) para las capturas del manual
--
-- Se ejecuta en una base de PRUEBA, después de base_datos.sql y
-- personalizador.sql. No lo ejecutes en la base real de la tienda.
--
--   psql -U postgres -c "CREATE DATABASE trajes_demo;"
--   psql -U postgres -d trajes_demo -f base_datos.sql
--   psql -U postgres -d trajes_demo -f personalizador.sql
--   psql -U postgres -d trajes_demo -f datos_demo.sql
--
-- Agrega: conjuntos de Puno, cuentas de la tienda y de clientes, y para el
-- cliente de prueba (usuario «cliente», DNI 44268151) una solicitud
-- pendiente y otra rechazada con su motivo, en fechas de la Candelaria 2027.
-- Usuarios nuevos: rosa.mamani / venta123 (VENDEDOR), ana.ramos y
-- jorge.calla / cliente123 (CLIENTE).
-- =====================================================================

SET client_encoding = 'UTF8';

BEGIN;

-- ---------------------------------------------------------------- conjuntos
INSERT INTO conjunto (nombre, id_danza, id_delegado, telefono)
SELECT v.nombre, d.id_danza,
       (SELECT id_cliente FROM cliente ORDER BY id_cliente OFFSET v.n LIMIT 1), v.telefono
FROM (VALUES
    ('Morenada Laykakota',               'Morenada',  29, '951203344'),
    ('Caporales Centralistas Puno',      'Caporales', 32, '973118820'),
    ('Diablada Bellavista',              'Diablada',  35, '984550612'),
    ('Tinkus Wayna Imilla',              'Tinkus',    38, '962774105'),
    ('Waca Waca Señor de Huanca',        'Waca Waca', 41, '957330298')
) AS v(nombre, danza, n, telefono)
JOIN danza d ON d.nombre = v.danza
ON CONFLICT (nombre) DO NOTHING;

-- ---------------------------------------------------------------- usuarios
INSERT INTO usuario (username, password_hash, rol, id_cliente) VALUES
('rosa.mamani', '$2a$10$moz1T4gCf7KAC9lOxzGS.OSMQeBf28/Zj9JstTPoJAYxpv7Nj2s2m', 'VENDEDOR', NULL),
('ana.ramos',   '$2a$10$WwDgWNmOw.yfBz2RttqeWePT2mBbEU3s37jjNRUL5ey7PE1EU1n0q', 'CLIENTE',
    (SELECT id_cliente FROM cliente WHERE nombres = 'Ana' AND apellidos = 'Ramos Chambi')),
('jorge.calla', '$2a$10$WwDgWNmOw.yfBz2RttqeWePT2mBbEU3s37jjNRUL5ey7PE1EU1n0q', 'CLIENTE',
    (SELECT id_cliente FROM cliente WHERE nombres = 'Jorge' AND apellidos = 'Calla Choque'))
ON CONFLICT (username) DO NOTHING;

-- ---------------------------------------------------------------- solicitudes del cliente de prueba
-- (se insertan en sentencias separadas: los triggers leen la cabecera del alquiler,
--  que no sería visible si todo fuera una sola sentencia con CTE)

-- 1) Rechazada: Kullawada para la entrada del domingo de octava
INSERT INTO alquiler (id_cliente, fecha_registro, fecha_salida, fecha_devolucion_prevista, estado, observacion, creado_en)
VALUES ((SELECT id_cliente FROM cliente WHERE numero_documento = '44268151'), CURRENT_DATE - 2,
        '2027-02-03', '2027-02-06', 'SOLICITADO', 'Para la entrada del domingo de octava',
        NOW() - INTERVAL '2 days 4 hours');
INSERT INTO detalle_alquiler (id_alquiler, id_traje, precio, garantia)
SELECT currval(pg_get_serial_sequence('alquiler', 'id_alquiler')), t.id_traje, t.precio_alquiler, t.garantia
FROM traje t
WHERE t.id_traje = (SELECT tt.id_traje FROM fn_trajes_disponibles('2027-02-03', '2027-02-06', 'Kullawada') f
                    JOIN traje tt ON tt.codigo = f.codigo ORDER BY tt.codigo LIMIT 1);
UPDATE alquiler SET estado = 'RECHAZADO',
       motivo_rechazo = 'Ese traje de kullawada se va a reparar el bordado antes de la octava; te ofrecemos el de talla S, que está libre en esas fechas.'
WHERE id_alquiler = currval(pg_get_serial_sequence('alquiler', 'id_alquiler'));

-- 2) Pendiente: dos trajes de Tinkus para la Parada de Veneración
INSERT INTO alquiler (id_cliente, fecha_registro, fecha_salida, fecha_devolucion_prevista, estado, observacion, creado_en)
VALUES ((SELECT id_cliente FROM cliente WHERE numero_documento = '44268151'), CURRENT_DATE,
        '2027-02-02', '2027-02-05', 'SOLICITADO', 'Para la Parada de Veneración; los recojo con mi hermano',
        NOW() - INTERVAL '3 hours 20 minutes');
INSERT INTO detalle_alquiler (id_alquiler, id_traje, precio, garantia)
SELECT currval(pg_get_serial_sequence('alquiler', 'id_alquiler')), t.id_traje, t.precio_alquiler, t.garantia
FROM traje t
WHERE t.id_traje IN (SELECT tt.id_traje FROM fn_trajes_disponibles('2027-02-02', '2027-02-05', 'Tinkus') f
                     JOIN traje tt ON tt.codigo = f.codigo ORDER BY tt.codigo LIMIT 2);

-- 3) Otra solicitud pendiente, de otro cliente (para la bandeja de la tienda)
INSERT INTO alquiler (id_cliente, fecha_registro, fecha_salida, fecha_devolucion_prevista, estado, observacion, creado_en)
VALUES ((SELECT id_cliente FROM cliente WHERE nombres = 'Ana' AND apellidos = 'Ramos Chambi'), CURRENT_DATE,
        '2027-02-01', '2027-02-04', 'SOLICITADO', 'Cholita caporal talla S', NOW() - INTERVAL '50 minutes');
INSERT INTO detalle_alquiler (id_alquiler, id_traje, precio, garantia)
SELECT currval(pg_get_serial_sequence('alquiler', 'id_alquiler')), t.id_traje, t.precio_alquiler, t.garantia
FROM traje t
WHERE t.id_traje = (SELECT tt.id_traje FROM fn_trajes_disponibles('2027-02-01', '2027-02-04', 'Caporales') f
                    JOIN traje tt ON tt.codigo = f.codigo WHERE tt.genero = 'MUJER' ORDER BY tt.codigo LIMIT 1);

COMMIT;

-- Comprobación
SELECT a.id_alquiler, c.nombres || ' ' || c.apellidos AS cliente, a.estado, a.fecha_salida, a.monto_alquiler,
       a.monto_garantia, a.motivo_rechazo
FROM alquiler a JOIN cliente c USING (id_cliente)
WHERE a.estado IN ('SOLICITADO', 'RECHAZADO')
ORDER BY a.id_alquiler;
