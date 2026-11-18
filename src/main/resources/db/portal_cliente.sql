-- Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor.

ALTER TABLE pedido_personalizado ADD COLUMN IF NOT EXISTS imagen_diseno BYTEA;

COMMENT ON COLUMN pedido_personalizado.imagen_diseno IS 'PNG del diseño 3D enviado por el cliente desde su portal';
