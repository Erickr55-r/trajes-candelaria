-- Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor.

ALTER TABLE alquiler ADD COLUMN IF NOT EXISTS creado_en TIMESTAMP;
UPDATE alquiler SET creado_en = fecha_registro::timestamp WHERE creado_en IS NULL;
ALTER TABLE alquiler ALTER COLUMN creado_en SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE alquiler ALTER COLUMN creado_en SET NOT NULL;
