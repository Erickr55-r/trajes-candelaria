-- Consultas de ejemplo y pruebas de reglas sobre la base alquiler_trajes

-- =====================================================================
-- CONSULTAS DE EJEMPLO (selecciona una y ejecútala)
-- =====================================================================

-- C1. Catálogo: cuántos trajes hay por danza y personaje
SELECT d.nombre AS danza, t.personaje, t.genero,
       COUNT(*) AS cantidad,
       STRING_AGG(t.talla, ', ' ORDER BY t.talla) AS tallas
FROM traje t
JOIN danza d ON d.id_danza = t.id_danza
GROUP BY d.nombre, t.personaje, t.genero
ORDER BY d.nombre, t.personaje;

-- C2. ¿Qué trajes de Caporales están libres para la Candelaria 2027?
--     (Prueba también con 'Diablada': ya está toda reservada y no devuelve filas.)
SELECT * FROM fn_trajes_disponibles('2027-01-29', '2027-02-12', 'Caporales');

-- C3. Trajes que están fuera ahora mismo (entregados) y cuándo deben volver
SELECT t.codigo, t.personaje, t.talla,
       CONCAT_WS(' ', c.nombres, c.apellidos) AS cliente,
       a.fecha_devolucion_prevista
FROM alquiler a
JOIN detalle_alquiler d ON d.id_alquiler = a.id_alquiler
JOIN traje t            ON t.id_traje = d.id_traje
JOIN cliente c          ON c.id_cliente = a.id_cliente
WHERE a.estado = 'ENTREGADO'
ORDER BY a.fecha_devolucion_prevista, t.codigo;

-- C4. Ingresos por alquiler en cada temporada de Candelaria
SELECT EXTRACT(YEAR FROM a.fecha_salida)::INT AS anio,
       COUNT(DISTINCT a.id_alquiler)          AS alquileres,
       COUNT(d.id_detalle)                    AS trajes_alquilados,
       SUM(d.precio)                          AS ingreso
FROM alquiler a
JOIN detalle_alquiler d ON d.id_alquiler = a.id_alquiler
WHERE a.estado <> 'ANULADO'
  AND EXTRACT(MONTH FROM a.fecha_salida) IN (1, 2)
GROUP BY EXTRACT(YEAR FROM a.fecha_salida)
ORDER BY anio;

-- C5. Danzas más rentables (todas las temporadas)
SELECT dz.nombre AS danza,
       COUNT(*)       AS veces_alquilado,
       SUM(d.precio)  AS ingreso_total,
       RANK() OVER (ORDER BY SUM(d.precio) DESC) AS puesto
FROM detalle_alquiler d
JOIN alquiler a ON a.id_alquiler = d.id_alquiler
JOIN traje t    ON t.id_traje = d.id_traje
JOIN danza dz   ON dz.id_danza = t.id_danza
WHERE a.estado <> 'ANULADO'
GROUP BY dz.nombre
ORDER BY puesto;

-- C6. Los 5 trajes más alquilados
SELECT codigo, danza, personaje, talla, veces_alquilado
FROM v_trajes_estado
ORDER BY veces_alquilado DESC, codigo
LIMIT 5;

-- C7. Trajes que nunca se alquilaron (LEFT JOIN ... IS NULL)
SELECT t.codigo, t.personaje, t.talla, t.estado
FROM traje t
LEFT JOIN detalle_alquiler d ON d.id_traje = t.id_traje
WHERE d.id_detalle IS NULL;

-- C8. Devoluciones con retraso: días de retraso y cliente
SELECT a.id_alquiler,
       CONCAT_WS(' ', c.nombres, c.apellidos) AS cliente,
       a.fecha_devolucion_prevista,
       a.fecha_devolucion_real,
       a.fecha_devolucion_real - a.fecha_devolucion_prevista AS dias_retraso
FROM alquiler a
JOIN cliente c ON c.id_cliente = a.id_cliente
WHERE a.fecha_devolucion_real > a.fecha_devolucion_prevista
ORDER BY dias_retraso DESC;

-- C9. Daños registrados: qué pieza, de qué traje y cuánto se cobró
SELECT a.id_alquiler, t.codigo, t.personaje, tp.nombre AS pieza,
       dn.tipo, dn.descripcion, dn.monto_cobro
FROM danio dn
JOIN detalle_alquiler d ON d.id_detalle = dn.id_detalle
JOIN alquiler a         ON a.id_alquiler = d.id_alquiler
JOIN traje t            ON t.id_traje = d.id_traje
JOIN pieza p            ON p.id_pieza = dn.id_pieza
JOIN tipo_pieza tp      ON tp.id_tipo_pieza = p.id_tipo_pieza
ORDER BY dn.monto_cobro DESC;

-- C10. Piezas que más se dañan
SELECT tp.nombre AS pieza, COUNT(*) AS veces_danada, SUM(dn.monto_cobro) AS total_cobrado
FROM danio dn
JOIN pieza p       ON p.id_pieza = dn.id_pieza
JOIN tipo_pieza tp ON tp.id_tipo_pieza = p.id_tipo_pieza
GROUP BY tp.nombre
ORDER BY veces_danada DESC;

-- C11. Liquidación de garantías: garantía cobrada, descuento por daños y devolución
SELECT id_alquiler, cliente, conjunto, monto_garantia, monto_danios,
       GREATEST(monto_garantia - monto_danios, 0) AS garantia_a_devolver,
       GREATEST(monto_danios - monto_garantia, 0) AS cobro_adicional
FROM v_alquileres_resumen
WHERE estado = 'DEVUELTO'
ORDER BY monto_danios DESC, id_alquiler;

-- C12. Reservas para la Candelaria 2027 con saldo pendiente
SELECT id_alquiler, cliente, conjunto, num_trajes, monto_alquiler, pagado_alquiler,
       monto_alquiler - pagado_alquiler AS saldo_pendiente
FROM v_alquileres_resumen
WHERE estado = 'RESERVADO'
ORDER BY saldo_pendiente DESC;

-- C13. Conjuntos y cuánto gastaron en total (clientes frecuentes)
SELECT cj.nombre AS conjunto, dz.nombre AS danza,
       COUNT(DISTINCT a.id_alquiler) AS alquileres,
       SUM(a.monto_alquiler)         AS total_gastado
FROM conjunto cj
JOIN danza dz        ON dz.id_danza = cj.id_danza
LEFT JOIN alquiler a ON a.id_conjunto = cj.id_conjunto AND a.estado <> 'ANULADO'
GROUP BY cj.nombre, dz.nombre
ORDER BY total_gastado DESC NULLS LAST;

-- C14. Ingresos por mes y acumulado (funciones de ventana)
SELECT TO_CHAR(fecha_pago, 'YYYY-MM') AS mes,
       SUM(monto) AS ingreso_mes,
       SUM(SUM(monto)) OVER (ORDER BY TO_CHAR(fecha_pago, 'YYYY-MM')) AS acumulado
FROM pago
WHERE concepto IN ('ADELANTO', 'SALDO', 'COBRO_DANIO')
GROUP BY TO_CHAR(fecha_pago, 'YYYY-MM')
ORDER BY mes;

-- C15. Costo de mantenimiento por traje frente a lo que generó (rentabilidad)
WITH ingreso AS (
    SELECT d.id_traje, SUM(d.precio) AS ingreso
    FROM detalle_alquiler d
    JOIN alquiler a ON a.id_alquiler = d.id_alquiler
    WHERE a.estado <> 'ANULADO'
    GROUP BY d.id_traje
),
gasto AS (
    SELECT id_traje, SUM(costo) AS gasto
    FROM mantenimiento
    GROUP BY id_traje
)
SELECT t.codigo, t.personaje, t.talla,
       COALESCE(i.ingreso, 0) AS ingreso,
       COALESCE(g.gasto, 0)   AS mantenimiento,
       COALESCE(i.ingreso, 0) - COALESCE(g.gasto, 0) AS ganancia
FROM traje t
LEFT JOIN ingreso i ON i.id_traje = t.id_traje
LEFT JOIN gasto g   ON g.id_traje = t.id_traje
ORDER BY ganancia DESC;


-- =====================================================================
-- PRUEBAS DE LAS REGLAS (cada una DEBE dar error; ejecútalas una por una)
-- =====================================================================

-- P1. Alquilar un traje ya reservado para la Candelaria 2027
--     Error esperado: "El traje ... ya está reservado en esas fechas"
-- BEGIN;
-- INSERT INTO alquiler (id_cliente, fecha_salida, fecha_devolucion_prevista)
-- VALUES (1, '2027-02-01', '2027-02-10');
-- INSERT INTO detalle_alquiler (id_alquiler, id_traje, precio, garantia)
-- SELECT currval('alquiler_id_alquiler_seq'), d.id_traje, 100, 100
-- FROM detalle_alquiler d JOIN alquiler a ON a.id_alquiler = d.id_alquiler
-- WHERE a.estado = 'RESERVADO' LIMIT 1;
-- ROLLBACK;

-- P2. Alquilar un traje que está en mantenimiento
--     Error esperado: "El traje ... no se puede alquilar: está en estado MANTENIMIENTO"
-- BEGIN;
-- INSERT INTO alquiler (id_cliente, fecha_salida, fecha_devolucion_prevista)
-- VALUES (1, '2026-11-10', '2026-11-12');
-- INSERT INTO detalle_alquiler (id_alquiler, id_traje, precio, garantia)
-- SELECT currval('alquiler_id_alquiler_seq'), id_traje, 100, 100
-- FROM traje WHERE estado = 'MANTENIMIENTO' LIMIT 1;
-- ROLLBACK;

-- P3. Devolución prevista antes de la salida
--     Error esperado: violates check constraint "alquiler_check"
-- INSERT INTO alquiler (id_cliente, fecha_salida, fecha_devolucion_prevista)
-- VALUES (1, '2027-02-10', '2027-02-01');

-- P4. Talla que no existe
--     Error esperado: violates check constraint "traje_talla_check"
-- UPDATE traje SET talla = 'XXL' WHERE codigo = 'CAP-001';
