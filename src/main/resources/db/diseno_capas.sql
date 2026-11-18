-- Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor.

SET client_encoding = 'UTF8';

BEGIN;

ALTER TABLE pedido_parte ALTER COLUMN id_color DROP NOT NULL;
ALTER TABLE pedido_parte ADD COLUMN IF NOT EXISTS color_hex CHAR(7);
ALTER TABLE pedido_parte DROP CONSTRAINT IF EXISTS pedido_parte_color_hex_check;
ALTER TABLE pedido_parte ADD CONSTRAINT pedido_parte_color_hex_check
    CHECK (color_hex IS NULL OR color_hex ~ '^#[0-9A-Fa-f]{6}$');
ALTER TABLE pedido_parte DROP CONSTRAINT IF EXISTS pedido_parte_algun_color_check;
ALTER TABLE pedido_parte ADD CONSTRAINT pedido_parte_algun_color_check
    CHECK (id_color IS NOT NULL OR color_hex IS NOT NULL);
UPDATE pedido_parte pp SET color_hex = c.hex FROM color c WHERE c.id_color = pp.id_color AND pp.color_hex IS NULL;

ALTER TABLE pedido_personalizado ADD COLUMN IF NOT EXISTS precio_estimado NUMERIC(10,2);
ALTER TABLE pedido_personalizado ADD COLUMN IF NOT EXISTS imagen_ruta VARCHAR(200);
UPDATE pedido_personalizado SET precio_estimado = monto_total WHERE precio_estimado IS NULL;

CREATE OR REPLACE VIEW v_pedidos_resumen AS
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

COMMIT;
