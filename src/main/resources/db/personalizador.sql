-- Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor.

SET client_encoding = 'UTF8';

DROP VIEW IF EXISTS v_pedidos_resumen CASCADE;
DROP TABLE IF EXISTS pedido_implemento, pedido_parte, pedido_personalizado,
                     implemento, color, parte_traje CASCADE;
DROP FUNCTION IF EXISTS fn_recalcular_pedido() CASCADE;
DROP FUNCTION IF EXISTS fn_validar_parte_pedido() CASCADE;

CREATE TABLE parte_traje (
    id_parte      SERIAL PRIMARY KEY,
    id_danza      INTEGER      NOT NULL REFERENCES danza(id_danza),
    nombre        VARCHAR(40)  NOT NULL,
    precio_base   NUMERIC(8,2) NOT NULL CHECK (precio_base >= 0),
    UNIQUE (id_danza, nombre)
);

CREATE TABLE color (
    id_color      SERIAL PRIMARY KEY,
    nombre        VARCHAR(30)  NOT NULL UNIQUE,
    hex           CHAR(7)      NOT NULL CHECK (hex ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE TABLE implemento (
    id_implemento SERIAL PRIMARY KEY,
    nombre        VARCHAR(50)  NOT NULL UNIQUE,
    precio        NUMERIC(8,2) NOT NULL CHECK (precio >= 0),
    parte_destino VARCHAR(40)
);

CREATE TABLE pedido_personalizado (
    id_pedido     SERIAL PRIMARY KEY,
    id_cliente    INTEGER       NOT NULL REFERENCES cliente(id_cliente),
    id_danza      INTEGER       NOT NULL REFERENCES danza(id_danza),
    talla         VARCHAR(3)    NOT NULL CHECK (talla IN ('XS', 'S', 'M', 'L', 'XL')),
    fecha_pedido  DATE          NOT NULL DEFAULT CURRENT_DATE,
    fecha_entrega DATE          NOT NULL,
    tipo          VARCHAR(10)   NOT NULL CHECK (tipo IN ('ALQUILER', 'CONFECCION')),
    estado        VARCHAR(15)   NOT NULL DEFAULT 'PENDIENTE'
                  CHECK (estado IN ('PENDIENTE', 'COTIZADO', 'APROBADO', 'EN_CONFECCION', 'LISTO',
                                    'ENTREGADO', 'CANCELADO')),
    monto_total   NUMERIC(10,2) NOT NULL DEFAULT 0,
    adelanto      NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (adelanto >= 0),
    observacion   VARCHAR(200),
    imagen_diseno BYTEA,
    imagen_ruta   VARCHAR(200),
    precio_estimado NUMERIC(10,2),
    CHECK (fecha_entrega >= fecha_pedido)
);

CREATE TABLE pedido_parte (
    id_pedido     INTEGER NOT NULL REFERENCES pedido_personalizado(id_pedido) ON DELETE CASCADE,
    id_parte      INTEGER NOT NULL REFERENCES parte_traje(id_parte),
    id_color      INTEGER REFERENCES color(id_color),
    color_hex     CHAR(7) CHECK (color_hex IS NULL OR color_hex ~ '^#[0-9A-Fa-f]{6}$'),
    CHECK (id_color IS NOT NULL OR color_hex IS NOT NULL),
    PRIMARY KEY (id_pedido, id_parte)
);

CREATE TABLE pedido_implemento (
    id_pedido     INTEGER NOT NULL REFERENCES pedido_personalizado(id_pedido) ON DELETE CASCADE,
    id_implemento INTEGER NOT NULL REFERENCES implemento(id_implemento),
    cantidad      INTEGER NOT NULL CHECK (cantidad > 0),
    PRIMARY KEY (id_pedido, id_implemento)
);

CREATE FUNCTION fn_validar_parte_pedido() RETURNS TRIGGER AS $$
DECLARE
    v_danza_pedido INTEGER;
    v_danza_parte  INTEGER;
BEGIN
    SELECT id_danza INTO v_danza_pedido FROM pedido_personalizado WHERE id_pedido = NEW.id_pedido;
    SELECT id_danza INTO v_danza_parte  FROM parte_traje          WHERE id_parte  = NEW.id_parte;
    IF v_danza_pedido IS DISTINCT FROM v_danza_parte THEN
        RAISE EXCEPTION 'La parte % no corresponde a la danza del pedido %.', NEW.id_parte, NEW.id_pedido;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_parte_pedido
BEFORE INSERT OR UPDATE ON pedido_parte
FOR EACH ROW EXECUTE FUNCTION fn_validar_parte_pedido();

CREATE FUNCTION fn_recalcular_pedido() RETURNS TRIGGER AS $$
DECLARE
    v_id INTEGER;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_id := OLD.id_pedido;
    ELSE
        v_id := NEW.id_pedido;
    END IF;

    UPDATE pedido_personalizado p
    SET monto_total = ROUND(
            ( COALESCE((SELECT SUM(pt.precio_base)
                        FROM pedido_parte pp
                        JOIN parte_traje pt ON pt.id_parte = pp.id_parte
                        WHERE pp.id_pedido = v_id), 0)
            + COALESCE((SELECT SUM(i.precio * pi.cantidad)
                        FROM pedido_implemento pi
                        JOIN implemento i ON i.id_implemento = pi.id_implemento
                        WHERE pi.id_pedido = v_id), 0) )
            * CASE WHEN p.tipo = 'ALQUILER' THEN 0.30 ELSE 1 END, 2)
    WHERE p.id_pedido = v_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_recalcular_pedido_parte
AFTER INSERT OR UPDATE OR DELETE ON pedido_parte
FOR EACH ROW EXECUTE FUNCTION fn_recalcular_pedido();

CREATE TRIGGER trg_recalcular_pedido_implemento
AFTER INSERT OR UPDATE OR DELETE ON pedido_implemento
FOR EACH ROW EXECUTE FUNCTION fn_recalcular_pedido();

CREATE VIEW v_pedidos_resumen AS
SELECT p.id_pedido,
       p.id_cliente,
       CONCAT_WS(' ', c.nombres, c.apellidos)                              AS cliente,
       p.id_danza,
       d.nombre                                                            AS danza,
       p.talla,
       p.tipo,
       p.estado,
       p.fecha_pedido,
       p.fecha_entrega,
       (SELECT COUNT(*) FROM pedido_parte pp WHERE pp.id_pedido = p.id_pedido)           AS num_partes,
       (SELECT COUNT(*) FROM pedido_implemento pi WHERE pi.id_pedido = p.id_pedido)      AS num_implementos,
       p.monto_total,
       p.adelanto,
       CASE WHEN p.estado = 'CANCELADO' THEN 0.00
            ELSE GREATEST(p.monto_total - p.adelanto, 0) END               AS saldo,
       p.observacion,
       p.precio_estimado
FROM pedido_personalizado p
JOIN cliente c ON c.id_cliente = p.id_cliente
JOIN danza d   ON d.id_danza = p.id_danza;

INSERT INTO parte_traje (id_danza, nombre, precio_base)
SELECT d.id_danza, v.parte, v.precio
FROM (VALUES
    ('Caporales', 'Chaqueta', 180.00), ('Caporales', 'Pantalón', 90.00), ('Caporales', 'Pollera', 120.00),
    ('Caporales', 'Botas', 150.00),    ('Caporales', 'Sombrero', 70.00), ('Caporales', 'Faja', 35.00),
    ('Morenada',  'Chaqueta', 260.00), ('Morenada',  'Pantalón', 95.00), ('Morenada',  'Pollera', 220.00),
    ('Morenada',  'Botas', 120.00),    ('Morenada',  'Sombrero', 80.00), ('Morenada',  'Máscara', 140.00),
    ('Diablada',  'Máscara', 320.00),  ('Diablada',  'Chaqueta', 240.00), ('Diablada', 'Pantalón', 90.00),
    ('Diablada',  'Botas', 130.00),    ('Diablada',  'Capa', 160.00),
    ('Tinkus',    'Chaqueta', 110.00), ('Tinkus',    'Pantalón', 70.00), ('Tinkus',   'Sombrero', 85.00),
    ('Tinkus',    'Faja', 40.00),      ('Tinkus',    'Botas', 60.00)
) AS v(danza, parte, precio)
JOIN danza d ON d.nombre = v.danza;

INSERT INTO color (nombre, hex) VALUES
('Rojo',           '#C62828'),
('Granate',        '#7B1E2B'),
('Dorado',         '#D4AF37'),
('Plateado',       '#C0C0C8'),
('Azul eléctrico', '#1E5BFF'),
('Verde',          '#2E8B3E'),
('Morado',         '#6A1B9A'),
('Negro',          '#1C1C1C'),
('Blanco',         '#F5F5F5'),
('Rosado',         '#F06292'),
('Naranja',        '#F57C00'),
('Celeste',        '#4FC3F7');

INSERT INTO implemento (nombre, precio, parte_destino) VALUES
('Cascabeles',     4.50, 'Botas'),
('Matraca',       85.00, 'Mano'),
('Pañuelo',       12.00, 'Mano'),
('Bordado dorado', 60.00, 'Chaqueta'),
('Lentejuelas',    0.80, 'Chaqueta'),
('Plumas',         8.00, 'Sombrero'),
('Espejos',        3.50, 'Sombrero'),
('Flecos dorados', 25.00, 'Faja');

INSERT INTO pedido_personalizado (id_cliente, id_danza, talla, fecha_pedido, fecha_entrega, tipo, estado, observacion)
SELECT (SELECT id_cliente FROM cliente ORDER BY id_cliente OFFSET v.n LIMIT 1),
       (SELECT id_danza FROM danza WHERE nombre = v.danza),
       v.talla, CURRENT_DATE + v.desde, CURRENT_DATE + v.hasta, v.tipo, v.estado, v.obs
FROM (VALUES
    (0, 'Caporales', 'M', -3,  25, 'CONFECCION', 'PENDIENTE',     'Caporal con botas de cascabeles para la Candelaria'),
    (1, 'Morenada',  'L', -12, 18, 'CONFECCION', 'EN_CONFECCION', 'Moreno con traje de rueda y matraca'),
    (2, 'Diablada',  'M', -20,  5, 'ALQUILER',   'LISTO',         'Diablo de capa morada'),
    (3, 'Caporales', 'S', -40, -10, 'CONFECCION', 'ENTREGADO',    'Cholita caporal de pollera rosada'),
    (4, 'Tinkus',    'M', -8,  30, 'ALQUILER',   'CANCELADO',     'El conjunto cambió de danza')
) AS v(n, danza, talla, desde, hasta, tipo, estado, obs)
ORDER BY v.n;

INSERT INTO pedido_parte (id_pedido, id_parte, id_color)
SELECT v.pedido, pt.id_parte, c.id_color
FROM (VALUES
    (1, 'Chaqueta', 'Rojo'),      (1, 'Pantalón', 'Blanco'),   (1, 'Botas', 'Blanco'),
    (1, 'Sombrero', 'Blanco'),    (1, 'Faja', 'Dorado'),
    (2, 'Chaqueta', 'Morado'),    (2, 'Pantalón', 'Negro'),    (2, 'Botas', 'Negro'),
    (2, 'Sombrero', 'Morado'),    (2, 'Máscara', 'Negro'),
    (3, 'Máscara', 'Rojo'),       (3, 'Chaqueta', 'Granate'),  (3, 'Pantalón', 'Negro'),
    (3, 'Botas', 'Negro'),        (3, 'Capa', 'Morado'),
    (4, 'Chaqueta', 'Rosado'),    (4, 'Pollera', 'Rosado'),    (4, 'Botas', 'Blanco'),
    (4, 'Sombrero', 'Blanco'),
    (5, 'Chaqueta', 'Verde'),     (5, 'Pantalón', 'Negro'),    (5, 'Sombrero', 'Naranja'),
    (5, 'Faja', 'Rojo'),          (5, 'Botas', 'Negro')
) AS v(pedido, parte, color)
JOIN pedido_personalizado p ON p.id_pedido = v.pedido
JOIN parte_traje pt ON pt.id_danza = p.id_danza AND pt.nombre = v.parte
JOIN color c ON c.nombre = v.color;

INSERT INTO pedido_implemento (id_pedido, id_implemento, cantidad)
SELECT v.pedido, i.id_implemento, v.cantidad
FROM (VALUES
    (1, 'Cascabeles', 24),   (1, 'Bordado dorado', 1),
    (2, 'Matraca', 1),       (2, 'Lentejuelas', 200),
    (3, 'Bordado dorado', 1), (3, 'Espejos', 10),
    (4, 'Cascabeles', 16),   (4, 'Pañuelo', 1),
    (5, 'Plumas', 6),        (5, 'Espejos', 8),     (5, 'Flecos dorados', 1)
) AS v(pedido, implemento, cantidad)
JOIN implemento i ON i.nombre = v.implemento;

UPDATE pedido_personalizado SET adelanto = ROUND(monto_total * 0.5, 2) WHERE id_pedido IN (1, 2);
UPDATE pedido_personalizado SET adelanto = monto_total WHERE id_pedido IN (3, 4);

UPDATE pedido_personalizado SET precio_estimado = monto_total;
UPDATE pedido_parte pp SET color_hex = c.hex FROM color c WHERE c.id_color = pp.id_color;

SELECT id_pedido, cliente, danza, tipo, estado, num_partes, num_implementos, monto_total, adelanto, saldo
FROM v_pedidos_resumen
ORDER BY id_pedido;
