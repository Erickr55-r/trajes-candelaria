-- Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor.

-- =====================================================================
-- INICIO DE SESIÓN CON ROLES + SOLICITUDES DE CLIENTES (PostgreSQL)
--
-- Migración para una base que YA tiene base_datos.sql y personalizador.sql.
-- No borra datos. Se puede ejecutar más de una vez.
-- (Las instalaciones nuevas ya traen estos cambios en base_datos.sql y
--  personalizador.sql; ahí no hace falta este archivo.)
--
--  1. alquiler: estados SOLICITADO y RECHAZADO + columna motivo_rechazo.
--  2. El trigger trg_validar_disponibilidad, fn_trajes_disponibles y la vista
--     v_trajes_estado cuentan SOLICITADO como ocupado.
--  3. pedido_personalizado: PENDIENTE, COTIZADO, APROBADO, EN_CONFECCION,
--     LISTO, ENTREGADO, CANCELADO (los ANULADO pasan a CANCELADO).
--  4. Tabla usuario (contraseñas BCrypt) y usuarios de prueba:
--       admin / admin123 (ADMIN) · vendedor / venta123 (VENDEDOR)
--       cliente / cliente123 (CLIENTE, vinculado al cliente con DNI 44268151)
--
-- Uso:  psql -U postgres -d alquiler_trajes -f login_roles.sql
-- =====================================================================

SET client_encoding = 'UTF8';

BEGIN;

-- ---------------------------------------------------------------- 1. alquiler
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN SELECT conname FROM pg_constraint
              WHERE conrelid = 'alquiler'::regclass AND contype = 'c'
                AND pg_get_constraintdef(oid) LIKE '%RESERVADO%'
    LOOP
        EXECUTE 'ALTER TABLE alquiler DROP CONSTRAINT ' || quote_ident(r.conname);
    END LOOP;
END $$;

ALTER TABLE alquiler ADD COLUMN IF NOT EXISTS motivo_rechazo VARCHAR(200);
ALTER TABLE alquiler DROP CONSTRAINT IF EXISTS alquiler_motivo_rechazo_check;
ALTER TABLE alquiler ADD CONSTRAINT alquiler_estado_check
    CHECK (estado IN ('SOLICITADO', 'RESERVADO', 'ENTREGADO', 'DEVUELTO', 'ANULADO', 'RECHAZADO'));
ALTER TABLE alquiler ADD CONSTRAINT alquiler_motivo_rechazo_check
    CHECK (estado <> 'RECHAZADO' OR motivo_rechazo IS NOT NULL);

-- ---------------------------------------------------------------- 2. disponibilidad
CREATE OR REPLACE FUNCTION fn_validar_disponibilidad() RETURNS TRIGGER AS $$
DECLARE
    v_salida   DATE;
    v_retorno  DATE;
    v_estado   VARCHAR(15);
    v_choque   INTEGER;
BEGIN
    SELECT fecha_salida, fecha_devolucion_prevista
      INTO v_salida, v_retorno
      FROM alquiler WHERE id_alquiler = NEW.id_alquiler;

    SELECT estado INTO v_estado FROM traje WHERE id_traje = NEW.id_traje;
    IF v_estado <> 'DISPONIBLE' THEN
        RAISE EXCEPTION 'El traje % no se puede alquilar: está en estado %', NEW.id_traje, v_estado;
    END IF;

    -- SOLICITADO también ocupa el traje: así dos clientes no piden el mismo traje.
    SELECT a.id_alquiler INTO v_choque
      FROM detalle_alquiler d
      JOIN alquiler a ON a.id_alquiler = d.id_alquiler
     WHERE d.id_traje = NEW.id_traje
       AND d.id_detalle <> COALESCE(NEW.id_detalle, -1)
       AND a.estado IN ('SOLICITADO', 'RESERVADO', 'ENTREGADO')
       AND a.fecha_salida <= v_retorno
       AND a.fecha_devolucion_prevista >= v_salida
     LIMIT 1;

    IF v_choque IS NOT NULL THEN
        RAISE EXCEPTION 'El traje % ya está reservado en esas fechas (alquiler %)', NEW.id_traje, v_choque;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_trajes_disponibles(p_desde DATE, p_hasta DATE, p_danza VARCHAR DEFAULT NULL)
RETURNS TABLE (codigo VARCHAR, danza VARCHAR, personaje VARCHAR, genero VARCHAR,
               talla VARCHAR, precio_alquiler NUMERIC, garantia NUMERIC) AS $$
    SELECT t.codigo, d.nombre, t.personaje, t.genero, t.talla, t.precio_alquiler, t.garantia
      FROM traje t
      JOIN danza d ON d.id_danza = t.id_danza
     WHERE t.estado = 'DISPONIBLE'
       AND (p_danza IS NULL OR d.nombre = p_danza)
       AND NOT EXISTS (SELECT 1
                         FROM detalle_alquiler da
                         JOIN alquiler a ON a.id_alquiler = da.id_alquiler
                        WHERE da.id_traje = t.id_traje
                          AND a.estado IN ('SOLICITADO', 'RESERVADO', 'ENTREGADO')
                          AND a.fecha_salida <= p_hasta
                          AND a.fecha_devolucion_prevista >= p_desde)
     ORDER BY d.nombre, t.personaje, t.talla;
$$ LANGUAGE sql;

CREATE OR REPLACE VIEW v_trajes_estado AS
SELECT t.codigo, d.nombre AS danza, t.personaje, t.genero, t.talla, t.estado,
       (SELECT MIN(a.fecha_salida)
          FROM detalle_alquiler da
          JOIN alquiler a ON a.id_alquiler = da.id_alquiler
         WHERE da.id_traje = t.id_traje
           AND a.estado IN ('SOLICITADO', 'RESERVADO', 'ENTREGADO')) AS proxima_salida,
       (SELECT COUNT(*) FROM detalle_alquiler da
         WHERE da.id_traje = t.id_traje)               AS veces_alquilado
FROM traje t
JOIN danza d ON d.id_danza = t.id_danza;

-- ---------------------------------------------------------------- 3. pedidos
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN SELECT conname FROM pg_constraint
              WHERE conrelid = 'pedido_personalizado'::regclass AND contype = 'c'
                AND pg_get_constraintdef(oid) LIKE '%PENDIENTE%'
    LOOP
        EXECUTE 'ALTER TABLE pedido_personalizado DROP CONSTRAINT ' || quote_ident(r.conname);
    END LOOP;
END $$;

UPDATE pedido_personalizado SET estado = 'CANCELADO' WHERE estado = 'ANULADO';
ALTER TABLE pedido_personalizado ADD CONSTRAINT pedido_personalizado_estado_check
    CHECK (estado IN ('PENDIENTE', 'COTIZADO', 'APROBADO', 'EN_CONFECCION', 'LISTO', 'ENTREGADO', 'CANCELADO'));

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
       p.observacion
FROM pedido_personalizado p
JOIN cliente c ON c.id_cliente = p.id_cliente
JOIN danza d   ON d.id_danza = p.id_danza;

-- ---------------------------------------------------------------- 4. usuarios
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario     SERIAL PRIMARY KEY,
    username       VARCHAR(30)  NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,                       -- BCrypt
    rol            VARCHAR(10)  NOT NULL CHECK (rol IN ('ADMIN', 'VENDEDOR', 'CLIENTE')),
    id_cliente     INTEGER      UNIQUE REFERENCES cliente(id_cliente),
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- un CLIENTE siempre está vinculado a un cliente; ADMIN y VENDEDOR pueden no estarlo
    CHECK (rol <> 'CLIENTE' OR id_cliente IS NOT NULL)
);

INSERT INTO usuario (username, password_hash, rol, id_cliente) VALUES
('admin',    '$2a$10$ULHsRAWwvjt4K7jCO8W6eOF2DsD9HzMmDi6OPOQvg6m5cgRvcJ6Ry', 'ADMIN',    NULL),
('vendedor', '$2a$10$moz1T4gCf7KAC9lOxzGS.OSMQeBf28/Zj9JstTPoJAYxpv7Nj2s2m', 'VENDEDOR', NULL),
('cliente',  '$2a$10$WwDgWNmOw.yfBz2RttqeWePT2mBbEU3s37jjNRUL5ey7PE1EU1n0q', 'CLIENTE',
    COALESCE((SELECT id_cliente FROM cliente WHERE numero_documento = '44268151'),
             (SELECT MIN(id_cliente) FROM cliente)))
ON CONFLICT (username) DO NOTHING;

COMMIT;

SELECT username, rol, id_cliente, activo FROM usuario ORDER BY id_usuario;
