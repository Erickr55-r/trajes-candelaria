/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.servicio;

import pe.ehuanca.trajes.modelo.AlquilerDetalleTraje;
import pe.ehuanca.trajes.modelo.AlquilerResumen;
import pe.ehuanca.trajes.modelo.DanioFila;
import pe.ehuanca.trajes.modelo.Ocupacion;
import pe.ehuanca.trajes.modelo.PagoFila;
import pe.ehuanca.trajes.modelo.PiezaAlquiler;
import pe.ehuanca.trajes.modelo.Resumen;
import pe.ehuanca.trajes.modelo.TrajeDisponible;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static pe.ehuanca.trajes.servicio.TrajeServicio.tieneTexto;
import static pe.ehuanca.trajes.servicio.TrajeServicio.vacioANulo;

/**
 * Alquileres: reserva, entrega, devolución con daños, pagos y anulación.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
@Service
public class AlquilerServicio {

    private static final BigDecimal DESCUENTO_CONJUNTO = new BigDecimal("0.90");

    private final JdbcTemplate jdbcErht;

    public AlquilerServicio(JdbcTemplate jdbc) {
        this.jdbcErht = jdbc;
    }

    private static final RowMapper<AlquilerResumen> RESUMEN = (rs, i) -> new AlquilerResumen(
            rs.getInt("id_alquiler"), rs.getInt("id_cliente"), rs.getString("estado"),
            Filas.fecha(rs, "fecha_salida"), Filas.fecha(rs, "fecha_devolucion_prevista"),
            Filas.fecha(rs, "fecha_devolucion_real"), rs.getString("cliente"), rs.getString("conjunto"),
            rs.getLong("num_trajes"), rs.getBigDecimal("monto_alquiler"), rs.getBigDecimal("monto_garantia"),
            rs.getBigDecimal("monto_danios"), rs.getBigDecimal("pagado_alquiler"), rs.getString("observacion"));

    public Resumen resumen() {
        return jdbcErht.queryForObject("""
                SELECT (SELECT COUNT(*) FROM traje WHERE estado = 'DISPONIBLE')      AS disponibles,
                       (SELECT COUNT(*) FROM traje WHERE estado = 'MANTENIMIENTO')   AS mantenimiento,
                       (SELECT COUNT(*) FROM alquiler WHERE estado = 'ENTREGADO')    AS fuera,
                       (SELECT COUNT(*) FROM alquiler WHERE estado = 'RESERVADO')    AS reservas,
                       (SELECT COUNT(*) FROM alquiler
                         WHERE estado = 'ENTREGADO' AND fecha_devolucion_prevista < CURRENT_DATE) AS vencidos,
                       (SELECT COALESCE(SUM(monto), 0) FROM pago
                         WHERE concepto IN ('ADELANTO', 'SALDO', 'COBRO_DANIO')
                           AND DATE_TRUNC('month', fecha_pago) = DATE_TRUNC('month', CURRENT_DATE)) AS ingresos_mes
                """, (rs, i) -> new Resumen(rs.getLong("disponibles"), rs.getLong("fuera"), rs.getLong("reservas"),
                rs.getLong("vencidos"), rs.getLong("mantenimiento"), rs.getBigDecimal("ingresos_mes")));
    }

    public List<AlquilerResumen> listar(String estado, String texto) {
        StringBuilder sqlErht = new StringBuilder("SELECT * FROM v_alquileres_resumen WHERE 1 = 1");
        List<Object> parametrosErht = new ArrayList<>();
        if (tieneTexto(estado)) {
            sqlErht.append(" AND estado = ?");
            parametrosErht.add(estado);
        }
        if (tieneTexto(texto)) {
            sqlErht.append(" AND (cliente ILIKE ? OR conjunto ILIKE ?)");
            String patronErht = "%" + texto.trim() + "%";
            parametrosErht.add(patronErht);
            parametrosErht.add(patronErht);
        }
        sqlErht.append(" ORDER BY fecha_salida DESC, id_alquiler DESC");
        return jdbcErht.query(sqlErht.toString(), RESUMEN, parametrosErht.toArray());
    }

    public List<AlquilerResumen> proximosMovimientos() {
        return jdbcErht.query("""
                SELECT * FROM v_alquileres_resumen
                WHERE (estado = 'RESERVADO' AND fecha_salida <= CURRENT_DATE + 30)
                   OR estado IN ('ENTREGADO', 'SOLICITADO')
                ORDER BY CASE estado WHEN 'SOLICITADO' THEN 0 ELSE 1 END,
                         CASE estado WHEN 'ENTREGADO' THEN fecha_devolucion_prevista ELSE fecha_salida END
                """, RESUMEN);
    }

    public List<AlquilerResumen> delCliente(int idCliente) {
        return jdbcErht.query("SELECT * FROM v_alquileres_resumen WHERE id_cliente = ? ORDER BY fecha_salida DESC",
                RESUMEN, idCliente);
    }

    public AlquilerResumen buscar(int idAlquiler) {
        List<AlquilerResumen> listaErht = jdbcErht.query(
                "SELECT * FROM v_alquileres_resumen WHERE id_alquiler = ?", RESUMEN, idAlquiler);
        if (listaErht.isEmpty()) {
            throw new ExcepcionReglaNegocio("No existe el alquiler " + idAlquiler);
        }
        return listaErht.get(0);
    }

    public List<AlquilerDetalleTraje> trajes(int idAlquiler) {
        return jdbcErht.query("""
                SELECT d.id_detalle, t.id_traje, t.codigo, dz.nombre AS danza, t.personaje, t.talla,
                       d.precio, d.garantia
                FROM detalle_alquiler d
                JOIN traje t  ON t.id_traje = d.id_traje
                JOIN danza dz ON dz.id_danza = t.id_danza
                WHERE d.id_alquiler = ?
                ORDER BY t.codigo
                """, (rs, i) -> new AlquilerDetalleTraje(rs.getInt("id_detalle"), rs.getInt("id_traje"),
                rs.getString("codigo"), rs.getString("danza"), rs.getString("personaje"), rs.getString("talla"),
                rs.getBigDecimal("precio"), rs.getBigDecimal("garantia")), idAlquiler);
    }

    public List<PagoFila> pagos(int idAlquiler) {
        return jdbcErht.query("""
                SELECT id_pago, fecha_pago, concepto, monto, metodo
                FROM pago
                WHERE id_alquiler = ?
                ORDER BY fecha_pago, id_pago
                """, (rs, i) -> new PagoFila(rs.getInt("id_pago"), Filas.fecha(rs, "fecha_pago"),
                rs.getString("concepto"), rs.getBigDecimal("monto"), rs.getString("metodo")), idAlquiler);
    }

    public List<DanioFila> danios(int idAlquiler) {
        return jdbcErht.query("""
                SELECT t.codigo, tp.nombre AS pieza, dn.tipo, dn.descripcion, dn.monto_cobro
                FROM danio dn
                JOIN detalle_alquiler d ON d.id_detalle = dn.id_detalle
                JOIN traje t            ON t.id_traje = d.id_traje
                JOIN pieza p            ON p.id_pieza = dn.id_pieza
                JOIN tipo_pieza tp      ON tp.id_tipo_pieza = p.id_tipo_pieza
                WHERE d.id_alquiler = ?
                ORDER BY t.codigo
                """, (rs, i) -> new DanioFila(rs.getString("codigo"), rs.getString("pieza"), rs.getString("tipo"),
                rs.getString("descripcion"), rs.getBigDecimal("monto_cobro")), idAlquiler);
    }

    public List<PiezaAlquiler> piezasDelAlquiler(int idAlquiler) {
        return jdbcErht.query("""
                SELECT p.id_pieza,
                       t.codigo || ' · ' || tp.nombre || ' (' || t.personaje || ', talla ' || t.talla || ')' AS etiqueta,
                       p.valor_reposicion
                FROM detalle_alquiler d
                JOIN traje t       ON t.id_traje = d.id_traje
                JOIN pieza p       ON p.id_traje = t.id_traje
                JOIN tipo_pieza tp ON tp.id_tipo_pieza = p.id_tipo_pieza
                WHERE d.id_alquiler = ?
                ORDER BY t.codigo, tp.nombre
                """, (rs, i) -> new PiezaAlquiler(rs.getInt("id_pieza"), rs.getString("etiqueta"),
                rs.getBigDecimal("valor_reposicion")), idAlquiler);
    }

    public List<TrajeDisponible> disponibles(LocalDate desde, LocalDate hasta, Integer idDanza, String talla) {
        StringBuilder sqlErht = new StringBuilder("""
                SELECT t.id_traje, t.codigo, d.nombre AS danza, t.personaje, t.genero, t.talla, t.color,
                       t.precio_alquiler, t.garantia
                FROM traje t
                JOIN danza d ON d.id_danza = t.id_danza
                WHERE t.estado = 'DISPONIBLE'
                  AND NOT EXISTS (SELECT 1
                                    FROM detalle_alquiler da
                                    JOIN alquiler a ON a.id_alquiler = da.id_alquiler
                                   WHERE da.id_traje = t.id_traje
                                     AND a.estado IN ('SOLICITADO', 'RESERVADO', 'ENTREGADO')
                                     AND a.fecha_salida <= ?
                                     AND a.fecha_devolucion_prevista >= ?)
                """);
        List<Object> parametrosErht = new ArrayList<>(List.of(hasta, desde));
        if (idDanza != null) {
            sqlErht.append(" AND t.id_danza = ?");
            parametrosErht.add(idDanza);
        }
        if (tieneTexto(talla)) {
            sqlErht.append(" AND t.talla = ?");
            parametrosErht.add(talla);
        }
        sqlErht.append(" ORDER BY d.nombre, t.personaje, t.talla, t.codigo");
        return jdbcErht.query(sqlErht.toString(), (rs, i) -> new TrajeDisponible(rs.getInt("id_traje"),
                rs.getString("codigo"), rs.getString("danza"), rs.getString("personaje"), rs.getString("genero"),
                rs.getString("talla"), rs.getString("color"), rs.getBigDecimal("precio_alquiler"),
                rs.getBigDecimal("garantia")), parametrosErht.toArray());
    }

    public List<String> codigosDisponibles(LocalDate desde, LocalDate hasta, String danza) {
        return jdbcErht.queryForList("SELECT codigo FROM fn_trajes_disponibles(?::date, ?::date, ?::varchar)",
                String.class, desde, hasta, vacioANulo(danza));
    }

    public List<Ocupacion> ocupaciones() {
        return jdbcErht.query("""
                SELECT da.id_traje, a.fecha_salida, a.fecha_devolucion_prevista, a.estado
                FROM detalle_alquiler da
                JOIN alquiler a ON a.id_alquiler = da.id_alquiler
                WHERE a.estado IN ('SOLICITADO', 'RESERVADO', 'ENTREGADO')
                ORDER BY da.id_traje, a.fecha_salida
                """, (rs, i) -> new Ocupacion(rs.getInt("id_traje"), rs.getDate("fecha_salida").toLocalDate(),
                rs.getDate("fecha_devolucion_prevista").toLocalDate(), rs.getString("estado")));
    }

    @Transactional
    public int crear(int idCliente, Integer idConjunto, LocalDate salida, LocalDate devolucion,
                     List<Integer> idTrajes, BigDecimal adelanto, String metodo, String observacion) {
        return crearConErht("RESERVADO", idCliente, idConjunto, salida, devolucion, idTrajes, adelanto, metodo, observacion);
    }

    @Transactional
    public int solicitar(int idCliente, LocalDate salida, LocalDate devolucion, List<Integer> idTrajes,
                         String observacion) {
        return solicitar(idCliente, null, salida, devolucion, idTrajes, observacion);
    }

    @Transactional
    public int solicitar(int idCliente, Integer idConjunto, LocalDate salida, LocalDate devolucion,
                         List<Integer> idTrajes, String observacion) {
        if (salida.isBefore(LocalDate.now())) {
            throw new ExcepcionReglaNegocio("La fecha de recojo no puede ser anterior a hoy.");
        }
        if (idConjunto != null) {
            Integer suyoErht = jdbcErht.queryForObject(
                    "SELECT COUNT(*) FROM conjunto WHERE id_conjunto = ? AND id_delegado = ?",
                    Integer.class, idConjunto, idCliente);
            if (suyoErht == null || suyoErht == 0) {
                throw new ExcepcionReglaNegocio("Solo puedes reservar a nombre de un conjunto del que eres delegado.");
            }
        }
        return crearConErht("SOLICITADO", idCliente, idConjunto, salida, devolucion, idTrajes, null, null, observacion);
    }

    public List<AlquilerDetalleTraje> trajesDelCliente(int idAlquiler, int idCliente) {
        Integer suyoErht = jdbcErht.queryForObject("SELECT COUNT(*) FROM alquiler WHERE id_alquiler = ? AND id_cliente = ?",
                Integer.class, idAlquiler, idCliente);
        if (suyoErht == null || suyoErht == 0) {
            return List.of();
        }
        return trajes(idAlquiler);
    }

    public List<pe.ehuanca.trajes.modelo.SolicitudFila> solicitudesPendientes() {
        return jdbcErht.query("""
                SELECT a.id_alquiler, a.id_cliente, CONCAT_WS(' ', c.nombres, c.apellidos) AS cliente, c.telefono,
                       cj.nombre AS conjunto, a.fecha_salida, a.fecha_devolucion_prevista,
                       (SELECT COUNT(*) FROM detalle_alquiler d WHERE d.id_alquiler = a.id_alquiler) AS num_trajes,
                       a.monto_alquiler, a.monto_garantia, a.observacion, a.creado_en
                FROM alquiler a
                JOIN cliente c        ON c.id_cliente = a.id_cliente
                LEFT JOIN conjunto cj ON cj.id_conjunto = a.id_conjunto
                WHERE a.estado = 'SOLICITADO'
                ORDER BY a.creado_en, a.id_alquiler
                """, (rs, i) -> new pe.ehuanca.trajes.modelo.SolicitudFila(rs.getInt("id_alquiler"), rs.getInt("id_cliente"),
                rs.getString("cliente"), rs.getString("telefono"), rs.getString("conjunto"),
                rs.getDate("fecha_salida").toLocalDate(), rs.getDate("fecha_devolucion_prevista").toLocalDate(),
                rs.getLong("num_trajes"), rs.getBigDecimal("monto_alquiler"), rs.getBigDecimal("monto_garantia"),
                rs.getString("observacion"), rs.getTimestamp("creado_en").toLocalDateTime()));
    }

    public int contarSolicitudes() {
        Integer n = jdbcErht.queryForObject("SELECT COUNT(*) FROM alquiler WHERE estado = 'SOLICITADO'", Integer.class);
        return n == null ? 0 : n;
    }

    public void aprobar(int idAlquiler) {
        int filasErht = jdbcErht.update("UPDATE alquiler SET estado = 'RESERVADO' WHERE id_alquiler = ? AND estado = 'SOLICITADO'",
                idAlquiler);
        if (filasErht == 0) {
            throw new ExcepcionReglaNegocio("Solo se pueden aprobar alquileres en estado SOLICITADO.");
        }
    }

    public void rechazar(int idAlquiler, String motivo) {
        if (!tieneTexto(motivo)) {
            throw new ExcepcionReglaNegocio("Escribe el motivo del rechazo: el cliente lo verá en su portal.");
        }
        int filasErht = jdbcErht.update("""
                UPDATE alquiler SET estado = 'RECHAZADO', motivo_rechazo = ?
                WHERE id_alquiler = ? AND estado = 'SOLICITADO'
                """, motivo.trim(), idAlquiler);
        if (filasErht == 0) {
            throw new ExcepcionReglaNegocio("Solo se pueden rechazar alquileres en estado SOLICITADO.");
        }
    }

    public void cancelarSolicitud(int idAlquiler, int idCliente) {
        int filasErht = jdbcErht.update("""
                UPDATE alquiler SET estado = 'ANULADO', observacion = COALESCE(observacion, 'Cancelada por el cliente')
                WHERE id_alquiler = ? AND id_cliente = ? AND estado = 'SOLICITADO'
                """, idAlquiler, idCliente);
        if (filasErht == 0) {
            throw new ExcepcionReglaNegocio("Solo puedes cancelar tus solicitudes que aún no fueron revisadas.");
        }
    }

    public java.util.Map<Integer, String> motivosRechazo(int idCliente) {
        java.util.Map<Integer, String> m = new java.util.HashMap<>();
        jdbcErht.query("SELECT id_alquiler, motivo_rechazo FROM alquiler WHERE id_cliente = ? AND estado = 'RECHAZADO'",
                rs -> {
                    m.put(rs.getInt(1), rs.getString(2));
                }, idCliente);
        return m;
    }

    public String motivoRechazo(int idAlquiler) {
        List<String> l = jdbcErht.queryForList("SELECT motivo_rechazo FROM alquiler WHERE id_alquiler = ?", String.class,
                idAlquiler);
        return l.isEmpty() ? null : l.get(0);
    }

    private int crearConErht(String estado, int idCliente, Integer idConjunto, LocalDate salida, LocalDate devolucion,
                         List<Integer> idTrajes, BigDecimal adelanto, String metodo, String observacion) {
        if (idTrajes == null || idTrajes.isEmpty()) {
            throw new ExcepcionReglaNegocio("Selecciona al menos un traje.");
        }
        if (devolucion.isBefore(salida)) {
            throw new ExcepcionReglaNegocio("La fecha de devolución no puede ser anterior a la de salida.");
        }
        Integer idAlquilerErht = jdbcErht.queryForObject("""
                INSERT INTO alquiler (id_cliente, id_conjunto, fecha_salida, fecha_devolucion_prevista, observacion, estado)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id_alquiler
                """, Integer.class, idCliente, idConjunto, salida, devolucion, vacioANulo(observacion), estado);

        BigDecimal factorErht = (idConjunto != null && idTrajes.size() >= 5) ? DESCUENTO_CONJUNTO : BigDecimal.ONE;
        for (Integer idTrajeErht : idTrajes) {
            jdbcErht.update("""
                    INSERT INTO detalle_alquiler (id_alquiler, id_traje, precio, garantia)
                    SELECT ?, id_traje, ROUND(precio_alquiler * ?, 2), garantia
                    FROM traje
                    WHERE id_traje = ?
                    """, idAlquilerErht, factorErht, idTrajeErht);
        }
        if (adelanto != null && adelanto.signum() > 0) {
            registrarPago(idAlquilerErht, "ADELANTO", adelanto, metodo);
        }
        return idAlquilerErht;
    }

    public void registrarPago(int idAlquiler, String concepto, BigDecimal monto, String metodo) {
        if (monto == null || monto.signum() <= 0) {
            throw new ExcepcionReglaNegocio("El monto del pago debe ser mayor que cero.");
        }
        jdbcErht.update("""
                INSERT INTO pago (id_alquiler, fecha_pago, concepto, monto, metodo)
                VALUES (?, CURRENT_DATE, ?, ?, ?)
                """, idAlquiler, concepto, monto, metodo);
    }

    @Transactional
    public void entregar(int idAlquiler, String metodo) {
        AlquilerResumen a = buscar(idAlquiler);
        if (!"RESERVADO".equals(a.estado())) {
            throw new ExcepcionReglaNegocio("Solo se pueden entregar alquileres en estado RESERVADO.");
        }
        if (a.saldo().signum() > 0) {
            registrarPago(idAlquiler, "SALDO", a.saldo(), metodo);
        }
        if (a.montoGarantia().signum() > 0) {
            registrarPago(idAlquiler, "GARANTIA", a.montoGarantia(), metodo);
        }
        jdbcErht.update("UPDATE alquiler SET estado = 'ENTREGADO' WHERE id_alquiler = ?", idAlquiler);
    }

    @Transactional
    public BigDecimal devolver(int idAlquiler, LocalDate fechaReal, String[] piezas, String[] tipos,
                               String[] descripciones, String[] montos, String metodo) {
        AlquilerResumen a = buscar(idAlquiler);
        if (!"ENTREGADO".equals(a.estado())) {
            throw new ExcepcionReglaNegocio("Solo se pueden devolver alquileres en estado ENTREGADO.");
        }
        if (fechaReal.isBefore(a.fechaSalida())) {
            throw new ExcepcionReglaNegocio("La fecha de devolución no puede ser anterior a la salida.");
        }

        BigDecimal totalDaniosErht = BigDecimal.ZERO;
        int filasErht = piezas == null ? 0 : piezas.length;
        for (int k = 0; k < filasErht; k++) {
            if (!tieneTexto(piezas[k])) {
                continue;
            }
            int idPiezaErht = Integer.parseInt(piezas[k].trim());
            String tipoErht = valorErht(tipos, k, "LEVE");
            String descripcionErht = valorErht(descripciones, k, null);
            BigDecimal montoErht = montoDanioErht(idPiezaErht, tipoErht, valorErht(montos, k, null));

            Integer idDetalleErht = jdbcErht.queryForObject("""
                    SELECT d.id_detalle
                    FROM detalle_alquiler d
                    JOIN pieza p ON p.id_traje = d.id_traje
                    WHERE d.id_alquiler = ? AND p.id_pieza = ?
                    """, Integer.class, idAlquiler, idPiezaErht);
            jdbcErht.update("""
                    INSERT INTO danio (id_detalle, id_pieza, tipo, descripcion, monto_cobro)
                    VALUES (?, ?, ?, ?, ?)
                    """, idDetalleErht, idPiezaErht, tipoErht, descripcionErht, montoErht);
            String estadoPiezaErht = switch (tipoErht) {
                case "PERDIDA" -> "PERDIDO";
                case "GRAVE" -> "DANADO";
                default -> "REGULAR";
            };
            jdbcErht.update("UPDATE pieza SET estado = ? WHERE id_pieza = ?", estadoPiezaErht, idPiezaErht);
            totalDaniosErht = totalDaniosErht.add(montoErht);
        }

        String observacionErht = a.observacion();
        long diasRetrasoErht = fechaReal.toEpochDay() - a.fechaDevolucionPrevista().toEpochDay();
        if (diasRetrasoErht > 0) {
            observacionErht = "Devuelto con " + diasRetrasoErht + " día(s) de retraso";
        }
        jdbcErht.update("""
                UPDATE alquiler
                SET estado = 'DEVUELTO', fecha_devolucion_real = ?, observacion = ?
                WHERE id_alquiler = ?
                """, fechaReal, observacionErht, idAlquiler);

        BigDecimal diferenciaErht = a.montoGarantia().subtract(totalDaniosErht);
        if (diferenciaErht.signum() > 0) {
            registrarPago(idAlquiler, "DEVOLUCION_GARANTIA", diferenciaErht, metodo);
        } else if (diferenciaErht.signum() < 0) {
            registrarPago(idAlquiler, "COBRO_DANIO", diferenciaErht.negate(), metodo);
        }
        return totalDaniosErht;
    }

    public void anular(int idAlquiler) {
        int filasErht = jdbcErht.update("""
                UPDATE alquiler
                SET estado = 'ANULADO', observacion = COALESCE(observacion, 'Anulado desde el sistema')
                WHERE id_alquiler = ? AND estado = 'RESERVADO'
                """, idAlquiler);
        if (filasErht == 0) {
            throw new ExcepcionReglaNegocio("Solo se pueden anular alquileres en estado RESERVADO.");
        }
    }

    private BigDecimal montoDanioErht(int idPieza, String tipo, String montoEscrito) {
        if (tieneTexto(montoEscrito)) {
            return new BigDecimal(montoEscrito.trim()).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal valorErht = jdbcErht.queryForObject(
                "SELECT valor_reposicion FROM pieza WHERE id_pieza = ?", BigDecimal.class, idPieza);
        BigDecimal porcentajeErht = switch (tipo) {
            case "PERDIDA" -> BigDecimal.ONE;
            case "GRAVE" -> new BigDecimal("0.35");
            default -> new BigDecimal("0.10");
        };
        return valorErht.multiply(porcentajeErht).setScale(2, RoundingMode.HALF_UP);
    }

    private static String valorErht(String[] arreglo, int k, String porDefecto) {
        if (arreglo == null || k >= arreglo.length || !tieneTexto(arreglo[k])) {
            return porDefecto;
        }
        return arreglo[k].trim();
    }
}
