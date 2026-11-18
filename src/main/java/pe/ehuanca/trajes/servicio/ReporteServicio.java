/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.servicio;

import pe.ehuanca.trajes.modelo.Tabla;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reportes para la gestión del negocio. Cada reporte es una consulta SQL.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
@Service
public class ReporteServicio {

    private final JdbcTemplate jdbc;

    public ReporteServicio(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Tabla> todos() {
        return List.of(
                tabla("Danzas más rentables",
                        "Ingresos por alquiler de cada danza, sin contar alquileres anulados.", """
                        SELECT RANK() OVER (ORDER BY SUM(d.precio) DESC) AS "Puesto",
                               dz.nombre                                 AS "Danza",
                               COUNT(*)                                  AS "Veces alquilado",
                               SUM(d.precio)                             AS "Ingreso (S/)"
                        FROM detalle_alquiler d
                        JOIN alquiler a ON a.id_alquiler = d.id_alquiler
                        JOIN traje t    ON t.id_traje = d.id_traje
                        JOIN danza dz   ON dz.id_danza = t.id_danza
                        WHERE a.estado NOT IN ('ANULADO', 'SOLICITADO', 'RECHAZADO')
                        GROUP BY dz.nombre
                        ORDER BY 1
                        """),
                tabla("Ingresos por mes",
                        "Adelantos, saldos y cobros por daños de los últimos 12 meses, con el acumulado.", """
                        SELECT TO_CHAR(fecha_pago, 'YYYY-MM') AS "Mes",
                               SUM(monto)                     AS "Ingreso (S/)",
                               SUM(SUM(monto)) OVER (ORDER BY TO_CHAR(fecha_pago, 'YYYY-MM')) AS "Acumulado (S/)"
                        FROM pago
                        WHERE concepto IN ('ADELANTO', 'SALDO', 'COBRO_DANIO')
                          AND fecha_pago >= CURRENT_DATE - INTERVAL '12 months'
                        GROUP BY TO_CHAR(fecha_pago, 'YYYY-MM')
                        ORDER BY 1
                        """),
                tabla("Trajes más alquilados",
                        "Los 10 trajes que más veces salieron.", """
                        SELECT codigo AS "Código", danza AS "Danza", personaje AS "Personaje",
                               talla AS "Talla", veces_alquilado AS "Veces"
                        FROM v_trajes_estado
                        ORDER BY veces_alquilado DESC, codigo
                        LIMIT 10
                        """),
                tabla("Piezas que más se dañan",
                        "Daños registrados en las devoluciones, agrupados por tipo de pieza.", """
                        SELECT tp.nombre        AS "Pieza",
                               COUNT(*)         AS "Veces dañada",
                               SUM(dn.monto_cobro) AS "Cobrado (S/)"
                        FROM danio dn
                        JOIN pieza p       ON p.id_pieza = dn.id_pieza
                        JOIN tipo_pieza tp ON tp.id_tipo_pieza = p.id_tipo_pieza
                        GROUP BY tp.nombre
                        ORDER BY 2 DESC
                        """),
                tabla("Devoluciones con retraso",
                        "Alquileres devueltos después de la fecha acordada.", """
                        SELECT a.id_alquiler AS "Alquiler",
                               CONCAT_WS(' ', c.nombres, c.apellidos) AS "Cliente",
                               TO_CHAR(a.fecha_devolucion_prevista, 'DD/MM/YYYY') AS "Debía volver",
                               TO_CHAR(a.fecha_devolucion_real, 'DD/MM/YYYY')     AS "Volvió",
                               a.fecha_devolucion_real - a.fecha_devolucion_prevista AS "Días de retraso"
                        FROM alquiler a
                        JOIN cliente c ON c.id_cliente = a.id_cliente
                        WHERE a.fecha_devolucion_real > a.fecha_devolucion_prevista
                        ORDER BY 5 DESC
                        """),
                tabla("Rentabilidad por traje",
                        "Lo que generó cada traje menos lo que costó su mantenimiento.", """
                        WITH ingreso AS (
                            SELECT d.id_traje, SUM(d.precio) AS ingreso
                            FROM detalle_alquiler d
                            JOIN alquiler a ON a.id_alquiler = d.id_alquiler
                            WHERE a.estado NOT IN ('ANULADO', 'SOLICITADO', 'RECHAZADO')
                            GROUP BY d.id_traje
                        ),
                        gasto AS (
                            SELECT id_traje, SUM(costo) AS gasto
                            FROM mantenimiento
                            GROUP BY id_traje
                        )
                        SELECT t.codigo AS "Código", t.personaje AS "Personaje", t.talla AS "Talla",
                               COALESCE(i.ingreso, 0) AS "Ingreso (S/)",
                               COALESCE(g.gasto, 0)   AS "Mantenimiento (S/)",
                               COALESCE(i.ingreso, 0) - COALESCE(g.gasto, 0) AS "Ganancia (S/)"
                        FROM traje t
                        LEFT JOIN ingreso i ON i.id_traje = t.id_traje
                        LEFT JOIN gasto g   ON g.id_traje = t.id_traje
                        ORDER BY 6 DESC
                        LIMIT 15
                        """),
                tablaPersonalizador("Pedidos personalizados por estado",
                        "Pedidos del personalizador 3D en cada estado, con lo cobrado y lo que falta cobrar.", """
                        SELECT estado AS "Estado",
                               COUNT(*) AS "Pedidos",
                               COUNT(*) FILTER (WHERE tipo = 'CONFECCION') AS "Confección",
                               COUNT(*) FILTER (WHERE tipo = 'ALQUILER')   AS "Alquiler",
                               SUM(monto_total) AS "Monto (S/)",
                               SUM(adelanto)    AS "Cobrado (S/)",
                               SUM(saldo)       AS "Saldo (S/)"
                        FROM v_pedidos_resumen
                        GROUP BY estado
                        ORDER BY ARRAY_POSITION(
                                 ARRAY['PENDIENTE', 'COTIZADO', 'APROBADO', 'EN_CONFECCION', 'LISTO', 'ENTREGADO',
                                       'CANCELADO']::VARCHAR[], estado)
                        """),
                tablaPersonalizador("Implementos más pedidos",
                        "Implementos elegidos en los pedidos personalizados (sin contar cancelados), a precio de confección.", """
                        SELECT RANK() OVER (ORDER BY COUNT(*) DESC, SUM(pi.cantidad) DESC) AS "Puesto",
                               i.nombre                     AS "Implemento",
                               i.parte_destino              AS "Va en",
                               COUNT(*)                     AS "Pedidos",
                               SUM(pi.cantidad)             AS "Unidades",
                               SUM(pi.cantidad * i.precio)  AS "Importe (S/)"
                        FROM pedido_implemento pi
                        JOIN implemento i           ON i.id_implemento = pi.id_implemento
                        JOIN pedido_personalizado p ON p.id_pedido = pi.id_pedido
                        WHERE p.estado <> 'CANCELADO'
                        GROUP BY i.nombre, i.parte_destino
                        ORDER BY 1, 2
                        """)
        );
    }

    /** Reporte sobre las tablas del personalizador: si aún no se ejecutó personalizador.sql, lo avisa. */
    private Tabla tablaPersonalizador(String titulo, String descripcion, String sql) {
        try {
            return tabla(titulo, descripcion, sql);
        } catch (DataAccessException e) {
            return new Tabla(titulo, "Falta ejecutar db/personalizador.sql sobre la base alquiler_trajes.",
                    new ArrayList<>(), new ArrayList<>());
        }
    }

    private Tabla tabla(String titulo, String descripcion, String sql) {
        List<Map<String, Object>> filas = jdbc.queryForList(sql);
        List<String> columnas = filas.isEmpty() ? new ArrayList<>() : new ArrayList<>(filas.get(0).keySet());
        List<List<Object>> valores = new ArrayList<>();
        for (Map<String, Object> fila : filas) {
            valores.add(new ArrayList<>(fila.values()));
        }
        return new Tabla(titulo, descripcion, columnas, valores);
    }
}
