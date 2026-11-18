-- Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor.

SELECT d.nombre AS danza, t.personaje, t.genero,
       COUNT(*) AS cantidad,
       STRING_AGG(t.talla, ', ' ORDER BY t.talla) AS tallas
FROM traje t
JOIN danza d ON d.id_danza = t.id_danza
GROUP BY d.nombre, t.personaje, t.genero
ORDER BY d.nombre, t.personaje;

SELECT * FROM fn_trajes_disponibles('2027-01-29', '2027-02-12', 'Caporales');

SELECT t.codigo, t.personaje, t.talla,
       CONCAT_WS(' ', c.nombres, c.apellidos) AS cliente,
       a.fecha_devolucion_prevista
FROM alquiler a
JOIN detalle_alquiler d ON d.id_alquiler = a.id_alquiler
JOIN traje t            ON t.id_traje = d.id_traje
JOIN cliente c          ON c.id_cliente = a.id_cliente
WHERE a.estado = 'ENTREGADO'
ORDER BY a.fecha_devolucion_prevista, t.codigo;

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

SELECT codigo, danza, personaje, talla, veces_alquilado
FROM v_trajes_estado
ORDER BY veces_alquilado DESC, codigo
LIMIT 5;

SELECT t.codigo, t.personaje, t.talla, t.estado
FROM traje t
LEFT JOIN detalle_alquiler d ON d.id_traje = t.id_traje
WHERE d.id_detalle IS NULL;

SELECT a.id_alquiler,
       CONCAT_WS(' ', c.nombres, c.apellidos) AS cliente,
       a.fecha_devolucion_prevista,
       a.fecha_devolucion_real,
       a.fecha_devolucion_real - a.fecha_devolucion_prevista AS dias_retraso
FROM alquiler a
JOIN cliente c ON c.id_cliente = a.id_cliente
WHERE a.fecha_devolucion_real > a.fecha_devolucion_prevista
ORDER BY dias_retraso DESC;

SELECT a.id_alquiler, t.codigo, t.personaje, tp.nombre AS pieza,
       dn.tipo, dn.descripcion, dn.monto_cobro
FROM danio dn
JOIN detalle_alquiler d ON d.id_detalle = dn.id_detalle
JOIN alquiler a         ON a.id_alquiler = d.id_alquiler
JOIN traje t            ON t.id_traje = d.id_traje
JOIN pieza p            ON p.id_pieza = dn.id_pieza
JOIN tipo_pieza tp      ON tp.id_tipo_pieza = p.id_tipo_pieza
ORDER BY dn.monto_cobro DESC;

SELECT tp.nombre AS pieza, COUNT(*) AS veces_danada, SUM(dn.monto_cobro) AS total_cobrado
FROM danio dn
JOIN pieza p       ON p.id_pieza = dn.id_pieza
JOIN tipo_pieza tp ON tp.id_tipo_pieza = p.id_tipo_pieza
GROUP BY tp.nombre
ORDER BY veces_danada DESC;

SELECT id_alquiler, cliente, conjunto, monto_garantia, monto_danios,
       GREATEST(monto_garantia - monto_danios, 0) AS garantia_a_devolver,
       GREATEST(monto_danios - monto_garantia, 0) AS cobro_adicional
FROM v_alquileres_resumen
WHERE estado = 'DEVUELTO'
ORDER BY monto_danios DESC, id_alquiler;

SELECT id_alquiler, cliente, conjunto, num_trajes, monto_alquiler, pagado_alquiler,
       monto_alquiler - pagado_alquiler AS saldo_pendiente
FROM v_alquileres_resumen
WHERE estado = 'RESERVADO'
ORDER BY saldo_pendiente DESC;

SELECT cj.nombre AS conjunto, dz.nombre AS danza,
       COUNT(DISTINCT a.id_alquiler) AS alquileres,
       SUM(a.monto_alquiler)         AS total_gastado
FROM conjunto cj
JOIN danza dz        ON dz.id_danza = cj.id_danza
LEFT JOIN alquiler a ON a.id_conjunto = cj.id_conjunto AND a.estado <> 'ANULADO'
GROUP BY cj.nombre, dz.nombre
ORDER BY total_gastado DESC NULLS LAST;

SELECT TO_CHAR(fecha_pago, 'YYYY-MM') AS mes,
       SUM(monto) AS ingreso_mes,
       SUM(SUM(monto)) OVER (ORDER BY TO_CHAR(fecha_pago, 'YYYY-MM')) AS acumulado
FROM pago
WHERE concepto IN ('ADELANTO', 'SALDO', 'COBRO_DANIO')
GROUP BY TO_CHAR(fecha_pago, 'YYYY-MM')
ORDER BY mes;

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
