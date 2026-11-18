-- Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor.

-- =====================================================================
-- PANEL DEL VENDEDOR (PostgreSQL)
-- Migración para una base que ya tiene base_datos.sql, personalizador.sql,
-- login_roles.sql y portal_cliente.sql. No borra datos. Se puede repetir.
-- (base_datos.sql ya trae esta columna para instalaciones nuevas.)
--
--  · alquiler.creado_en: fecha y hora en que se registró el alquiler o la
--    solicitud (para mostrar la antigüedad: "hace 2 h"). A los alquileres
--    que ya existían se les pone su fecha_registro.
--
-- Uso:  psql -U postgres -d alquiler_trajes -f panel_vendedor.sql
-- =====================================================================

ALTER TABLE alquiler ADD COLUMN IF NOT EXISTS creado_en TIMESTAMP;
UPDATE alquiler SET creado_en = fecha_registro::timestamp WHERE creado_en IS NULL;
ALTER TABLE alquiler ALTER COLUMN creado_en SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE alquiler ALTER COLUMN creado_en SET NOT NULL;
