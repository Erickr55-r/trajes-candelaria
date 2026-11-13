-- =====================================================================
-- PORTAL DEL CLIENTE (PostgreSQL)
-- Migración para una base que ya tiene base_datos.sql, personalizador.sql
-- y login_roles.sql. No borra datos. Se puede ejecutar más de una vez.
-- (personalizador.sql ya trae esta columna para instalaciones nuevas.)
--
--  · pedido_personalizado.imagen_diseno: imagen PNG del diseño 3D que el
--    cliente envía desde su portal ("Enviar mi diseño a la tienda").
--
-- Uso:  psql -U postgres -d alquiler_trajes -f portal_cliente.sql
-- =====================================================================

ALTER TABLE pedido_personalizado ADD COLUMN IF NOT EXISTS imagen_diseno BYTEA;

COMMENT ON COLUMN pedido_personalizado.imagen_diseno IS 'PNG del diseño 3D enviado por el cliente desde su portal';
