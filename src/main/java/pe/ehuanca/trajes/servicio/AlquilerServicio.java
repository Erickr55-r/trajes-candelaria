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

    /** Descuento por conjunto: se cobra el 90% del precio (10% menos) a conjuntos que alquilan 5 trajes o más. */
    private static final BigDecimal DESCUENTO_CONJUNTO = new BigDecimal("0.90");

    private final JdbcTemplate jdbc;

    public AlquilerServicio(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<AlquilerResumen> RESUMEN = (rs, i) -> new AlquilerResumen(
            rs.getInt("id_alquiler"), rs.getInt("id_cliente"), rs.getString("estado"),
            Filas.fecha(rs, "fecha_salida"), Filas.fecha(rs, "fecha_devolucion_prevista"),
            Filas.fecha(rs, "fecha_devolucion_real"), rs.getString("cliente"), rs.getString("conjunto"),
            rs.getLong("num_trajes"), rs.getBigDecimal("monto_alquiler"), rs.getBigDecimal("monto_garantia"),
            rs.getBigDecimal("monto_danios"), rs.getBigDecimal("pagado_alquiler"), rs.getString("observacion"));

    // ------------------------------------------------------------------ consultas

    public Resumen resumen() {
        return jdbc.queryForObject("""
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
        StringBuilder sql = new StringBuilder("SELECT * FROM v_alquileres_resumen WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (tieneTexto(estado)) {
            sql.append(" AND estado = ?");
            parametros.add(estado);
        }
        if (tieneTexto(texto)) {
            sql.append(" AND (cliente ILIKE ? OR conjunto ILIKE ?)");
            String patron = "%" + texto.trim() + "%";
            parametros.add(patron);
            parametros.add(patron);
        }
        sql.append(" ORDER BY fecha_salida DESC, id_alquiler DESC");
        return jdbc.query(sql.toString(), RESUMEN, parametros.toArray());
    }

    /** Alquileres que salen o deben volver en los próximos días (pantalla de inicio). */
    public List<AlquilerResumen> proximosMovimientos() {
        return jdbc.query("""
                SELECT * FROM v_alquileres_resumen
                WHERE (estado = 'RESERVADO' AND fecha_salida <= CURRENT_DATE + 30)
                   OR estado IN ('ENTREGADO', 'SOLICITADO')
                ORDER BY CASE estado WHEN 'SOLICITADO' THEN 0 ELSE 1 END,
                         CASE estado WHEN 'ENTREGADO' THEN fecha_devolucion_prevista ELSE fecha_salida END
                """, RESUMEN);
    }

    public List<AlquilerResumen> delCliente(int idCliente) {
        return jdbc.query("SELECT * FROM v_alquileres_resumen WHERE id_cliente = ? ORDER BY fecha_salida DESC",
                RESUMEN, idCliente);
    }

    public AlquilerResumen buscar(int idAlquiler) {
        List<AlquilerResumen> lista = jdbc.query(
                "SELECT * FROM v_alquileres_resumen WHERE id_alquiler = ?", RESUMEN, idAlquiler);
        if (lista.isEmpty()) {
            throw new ExcepcionReglaNegocio("No existe el alquiler " + idAlquiler);
        }
        return lista.get(0);
    }

    public List<AlquilerDetalleTraje> trajes(int idAlquiler) {
        return jdbc.query("""
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
        return jdbc.query("""
                SELECT id_pago, fecha_pago, concepto, monto, metodo
                FROM pago
                WHERE id_alquiler = ?
                ORDER BY fecha_pago, id_pago
                """, (rs, i) -> new PagoFila(rs.getInt("id_pago"), Filas.fecha(rs, "fecha_pago"),
                rs.getString("concepto"), rs.getBigDecimal("monto"), rs.getString("metodo")), idAlquiler);
    }

    public List<DanioFila> danios(int idAlquiler) {
        return jdbc.query("""
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

    /** Piezas de los trajes de un alquiler, para marcar daños al devolver. */
    public List<PiezaAlquiler> piezasDelAlquiler(int idAlquiler) {
        return jdbc.query("""
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

    /**
     * Trajes libres entre dos fechas (misma lógica que fn_trajes_disponibles).
     * Disponibilidad: el traje debe estar DISPONIBLE y ninguna reserva activa (SOLICITADO, RESERVADO o
     * ENTREGADO) puede cruzarse con el rango; dos rangos se cruzan si salida ≤ hasta y devolución ≥ desde.
     */
    public List<TrajeDisponible> disponibles(LocalDate desde, LocalDate hasta, Integer idDanza, String talla) {
        StringBuilder sql = new StringBuilder("""
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
        List<Object> parametros = new ArrayList<>(List.of(hasta, desde));
        if (idDanza != null) {
            sql.append(" AND t.id_danza = ?");
            parametros.add(idDanza);
        }
        if (tieneTexto(talla)) {
            sql.append(" AND t.talla = ?");
            parametros.add(talla);
        }
        sql.append(" ORDER BY d.nombre, t.personaje, t.talla, t.codigo");
        return jdbc.query(sql.toString(), (rs, i) -> new TrajeDisponible(rs.getInt("id_traje"),
                rs.getString("codigo"), rs.getString("danza"), rs.getString("personaje"), rs.getString("genero"),
                rs.getString("talla"), rs.getString("color"), rs.getBigDecimal("precio_alquiler"),
                rs.getBigDecimal("garantia")), parametros.toArray());
    }

    /** Códigos de los trajes libres según la función fn_trajes_disponibles de la base de datos. */
    public List<String> codigosDisponibles(LocalDate desde, LocalDate hasta, String danza) {
        return jdbc.queryForList("SELECT codigo FROM fn_trajes_disponibles(?::date, ?::date, ?::varchar)",
                String.class, desde, hasta, vacioANulo(danza));
    }

    /** Alquileres solicitados, reservados o entregados (aún no devueltos) de cada traje, para saber cuándo queda libre. */
    public List<Ocupacion> ocupaciones() {
        return jdbc.query("""
                SELECT da.id_traje, a.fecha_salida, a.fecha_devolucion_prevista, a.estado
                FROM detalle_alquiler da
                JOIN alquiler a ON a.id_alquiler = da.id_alquiler
                WHERE a.estado IN ('SOLICITADO', 'RESERVADO', 'ENTREGADO')
                ORDER BY da.id_traje, a.fecha_salida
                """, (rs, i) -> new Ocupacion(rs.getInt("id_traje"), rs.getDate("fecha_salida").toLocalDate(),
                rs.getDate("fecha_devolucion_prevista").toLocalDate(), rs.getString("estado")));
    }

    // ------------------------------------------------------------------ operaciones

    /**
     * Registra una reserva con sus trajes. El trigger de la base de datos rechaza
     * cualquier traje que ya esté ocupado en esas fechas; si eso pasa, se deshace todo.
     */
    @Transactional
    public int crear(int idCliente, Integer idConjunto, LocalDate salida, LocalDate devolucion,
                     List<Integer> idTrajes, BigDecimal adelanto, String metodo, String observacion) {
        return crearCon("RESERVADO", idCliente, idConjunto, salida, devolucion, idTrajes, adelanto, metodo, observacion);
    }

    /**
     * Solicitud hecha por el cliente desde su portal: queda SOLICITADO (los trajes ya quedan
     * apartados para que nadie más los pida) hasta que la tienda la apruebe o la rechace.
     */
    @Transactional
    public int solicitar(int idCliente, LocalDate salida, LocalDate devolucion, List<Integer> idTrajes,
                         String observacion) {
        return solicitar(idCliente, null, salida, devolucion, idTrajes, observacion);
    }

    /**
     * Igual que {@link #solicitar(int, LocalDate, LocalDate, List, String)} pero a nombre de un conjunto
     * del que el cliente es delegado (así aplica el 10% desde 5 trajes, igual que en la tienda).
     */
    @Transactional
    public int solicitar(int idCliente, Integer idConjunto, LocalDate salida, LocalDate devolucion,
                         List<Integer> idTrajes, String observacion) {
        if (salida.isBefore(LocalDate.now())) {
            throw new ExcepcionReglaNegocio("La fecha de recojo no puede ser anterior a hoy.");
        }
        if (idConjunto != null) {
            Integer suyo = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM conjunto WHERE id_conjunto = ? AND id_delegado = ?",
                    Integer.class, idConjunto, idCliente);
            if (suyo == null || suyo == 0) {
                throw new ExcepcionReglaNegocio("Solo puedes reservar a nombre de un conjunto del que eres delegado.");
            }
        }
        return crearCon("SOLICITADO", idCliente, idConjunto, salida, devolucion, idTrajes, null, null, observacion);
    }

    /** Trajes de un alquiler, solo si el alquiler es del cliente indicado (portal del cliente). */
    public List<AlquilerDetalleTraje> trajesDelCliente(int idAlquiler, int idCliente) {
        Integer suyo = jdbc.queryForObject("SELECT COUNT(*) FROM alquiler WHERE id_alquiler = ? AND id_cliente = ?",
                Integer.class, idAlquiler, idCliente);
        if (suyo == null || suyo == 0) {
            return List.of();
        }
        return trajes(idAlquiler);
    }

    /** Solicitudes de clientes por aprobar, de la más antigua a la más reciente. */
    public List<pe.ehuanca.trajes.modelo.SolicitudFila> solicitudesPendientes() {
        return jdbc.query("""
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

    /** Número de solicitudes por aprobar (insignia del menú e indicador de Inicio). */
    public int contarSolicitudes() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM alquiler WHERE estado = 'SOLICITADO'", Integer.class);
        return n == null ? 0 : n;
    }

    /** La tienda aprueba la solicitud: pasa a RESERVADO. */
    public void aprobar(int idAlquiler) {
        int filas = jdbc.update("UPDATE alquiler SET estado = 'RESERVADO' WHERE id_alquiler = ? AND estado = 'SOLICITADO'",
                idAlquiler);
        if (filas == 0) {
            throw new ExcepcionReglaNegocio("Solo se pueden aprobar alquileres en estado SOLICITADO.");
        }
    }

    /** La tienda rechaza la solicitud con un motivo (los trajes quedan libres). */
    public void rechazar(int idAlquiler, String motivo) {
        if (!tieneTexto(motivo)) {
            throw new ExcepcionReglaNegocio("Escribe el motivo del rechazo: el cliente lo verá en su portal.");
        }
        int filas = jdbc.update("""
                UPDATE alquiler SET estado = 'RECHAZADO', motivo_rechazo = ?
                WHERE id_alquiler = ? AND estado = 'SOLICITADO'
                """, motivo.trim(), idAlquiler);
        if (filas == 0) {
            throw new ExcepcionReglaNegocio("Solo se pueden rechazar alquileres en estado SOLICITADO.");
        }
    }

    /** El cliente retira su propia solicitud mientras sigue pendiente. */
    public void cancelarSolicitud(int idAlquiler, int idCliente) {
        int filas = jdbc.update("""
                UPDATE alquiler SET estado = 'ANULADO', observacion = COALESCE(observacion, 'Cancelada por el cliente')
                WHERE id_alquiler = ? AND id_cliente = ? AND estado = 'SOLICITADO'
                """, idAlquiler, idCliente);
        if (filas == 0) {
            throw new ExcepcionReglaNegocio("Solo puedes cancelar tus solicitudes que aún no fueron revisadas.");
        }
    }

    /** Motivo de rechazo de cada alquiler rechazado del cliente. */
    public java.util.Map<Integer, String> motivosRechazo(int idCliente) {
        java.util.Map<Integer, String> m = new java.util.HashMap<>();
        jdbc.query("SELECT id_alquiler, motivo_rechazo FROM alquiler WHERE id_cliente = ? AND estado = 'RECHAZADO'",
                rs -> {
                    m.put(rs.getInt(1), rs.getString(2));
                }, idCliente);
        return m;
    }

    public String motivoRechazo(int idAlquiler) {
        List<String> l = jdbc.queryForList("SELECT motivo_rechazo FROM alquiler WHERE id_alquiler = ?", String.class,
                idAlquiler);
        return l.isEmpty() ? null : l.get(0);
    }

    /**
     * Crea el alquiler con sus trajes. Los montos se guardan en detalle_alquiler copiando el precio y la
     * garantía vigentes de cada traje; los triggers de la base suman los totales del alquiler.
     */
    private int crearCon(String estado, int idCliente, Integer idConjunto, LocalDate salida, LocalDate devolucion,
                         List<Integer> idTrajes, BigDecimal adelanto, String metodo, String observacion) {
        if (idTrajes == null || idTrajes.isEmpty()) {
            throw new ExcepcionReglaNegocio("Selecciona al menos un traje.");
        }
        if (devolucion.isBefore(salida)) {
            throw new ExcepcionReglaNegocio("La fecha de devolución no puede ser anterior a la de salida.");
        }
        Integer idAlquiler = jdbc.queryForObject("""
                INSERT INTO alquiler (id_cliente, id_conjunto, fecha_salida, fecha_devolucion_prevista, observacion, estado)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id_alquiler
                """, Integer.class, idCliente, idConjunto, salida, devolucion, vacioANulo(observacion), estado);

        // Descuento por conjunto: solo si el alquiler va a nombre de un conjunto y lleva 5 trajes o más.
        // Se aplica al precio de cada traje (redondeado a 2 decimales); la garantía nunca tiene descuento.
        BigDecimal factor = (idConjunto != null && idTrajes.size() >= 5) ? DESCUENTO_CONJUNTO : BigDecimal.ONE;
        for (Integer idTraje : idTrajes) {
            jdbc.update("""
                    INSERT INTO detalle_alquiler (id_alquiler, id_traje, precio, garantia)
                    SELECT ?, id_traje, ROUND(precio_alquiler * ?, 2), garantia
                    FROM traje
                    WHERE id_traje = ?
                    """, idAlquiler, factor, idTraje);
        }
        if (adelanto != null && adelanto.signum() > 0) {
            registrarPago(idAlquiler, "ADELANTO", adelanto, metodo);
        }
        return idAlquiler;
    }

    public void registrarPago(int idAlquiler, String concepto, BigDecimal monto, String metodo) {
        if (monto == null || monto.signum() <= 0) {
            throw new ExcepcionReglaNegocio("El monto del pago debe ser mayor que cero.");
        }
        jdbc.update("""
                INSERT INTO pago (id_alquiler, fecha_pago, concepto, monto, metodo)
                VALUES (?, CURRENT_DATE, ?, ?, ?)
                """, idAlquiler, concepto, monto, metodo);
    }

    /** Entrega los trajes: cobra el saldo pendiente y la garantía. */
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
        jdbc.update("UPDATE alquiler SET estado = 'ENTREGADO' WHERE id_alquiler = ?", idAlquiler);
    }

    /**
     * Registra la devolución: guarda los daños, cambia el estado de las piezas
     * y liquida la garantía (devuelve la diferencia o cobra lo que falte).
     *
     * @param piezas  id de pieza de cada fila de daño (vacío = fila sin usar)
     * @param tipos   LEVE, GRAVE o PERDIDA
     * @param montos  monto a cobrar; si está vacío se calcula según el tipo
     */
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

        BigDecimal totalDanios = BigDecimal.ZERO;
        int filas = piezas == null ? 0 : piezas.length;
        for (int k = 0; k < filas; k++) {
            if (!tieneTexto(piezas[k])) {
                continue;
            }
            int idPieza = Integer.parseInt(piezas[k].trim());
            String tipo = valor(tipos, k, "LEVE");
            String descripcion = valor(descripciones, k, null);
            // Daños: cada fila registra la pieza dañada, el tipo y el monto a cobrar.
            BigDecimal monto = montoDanio(idPieza, tipo, valor(montos, k, null));

            Integer idDetalle = jdbc.queryForObject("""
                    SELECT d.id_detalle
                    FROM detalle_alquiler d
                    JOIN pieza p ON p.id_traje = d.id_traje
                    WHERE d.id_alquiler = ? AND p.id_pieza = ?
                    """, Integer.class, idAlquiler, idPieza);
            jdbc.update("""
                    INSERT INTO danio (id_detalle, id_pieza, tipo, descripcion, monto_cobro)
                    VALUES (?, ?, ?, ?, ?)
                    """, idDetalle, idPieza, tipo, descripcion, monto);
            // La pieza queda marcada según el daño: perdida, dañada (grave) o en estado regular (leve).
            String estadoPieza = switch (tipo) {
                case "PERDIDA" -> "PERDIDO";
                case "GRAVE" -> "DANADO";
                default -> "REGULAR";
            };
            jdbc.update("UPDATE pieza SET estado = ? WHERE id_pieza = ?", estadoPieza, idPieza);
            totalDanios = totalDanios.add(monto);
        }

        String observacion = a.observacion();
        long diasRetraso = fechaReal.toEpochDay() - a.fechaDevolucionPrevista().toEpochDay();
        if (diasRetraso > 0) {
            observacion = "Devuelto con " + diasRetraso + " día(s) de retraso";
        }
        jdbc.update("""
                UPDATE alquiler
                SET estado = 'DEVUELTO', fecha_devolucion_real = ?, observacion = ?
                WHERE id_alquiler = ?
                """, fechaReal, observacion, idAlquiler);

        // Liquidación de la garantía: si los daños son menores se devuelve la diferencia al cliente;
        // si la superan, se cobra lo que falta. Si son iguales no se registra ningún pago.
        BigDecimal diferencia = a.montoGarantia().subtract(totalDanios);
        if (diferencia.signum() > 0) {
            registrarPago(idAlquiler, "DEVOLUCION_GARANTIA", diferencia, metodo);
        } else if (diferencia.signum() < 0) {
            registrarPago(idAlquiler, "COBRO_DANIO", diferencia.negate(), metodo);
        }
        return totalDanios;
    }

    public void anular(int idAlquiler) {
        int filas = jdbc.update("""
                UPDATE alquiler
                SET estado = 'ANULADO', observacion = COALESCE(observacion, 'Anulado desde el sistema')
                WHERE id_alquiler = ? AND estado = 'RESERVADO'
                """, idAlquiler);
        if (filas == 0) {
            throw new ExcepcionReglaNegocio("Solo se pueden anular alquileres en estado RESERVADO.");
        }
    }

    /**
     * Monto del daño. Si no se escribe un monto, se cobra un porcentaje del valor de reposición de la pieza:
     * 100% si se perdió, 35% si el daño es grave y 10% si es leve.
     */
    private BigDecimal montoDanio(int idPieza, String tipo, String montoEscrito) {
        if (tieneTexto(montoEscrito)) {
            return new BigDecimal(montoEscrito.trim()).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal valor = jdbc.queryForObject(
                "SELECT valor_reposicion FROM pieza WHERE id_pieza = ?", BigDecimal.class, idPieza);
        BigDecimal porcentaje = switch (tipo) {
            case "PERDIDA" -> BigDecimal.ONE;
            case "GRAVE" -> new BigDecimal("0.35");
            default -> new BigDecimal("0.10");
        };
        return valor.multiply(porcentaje).setScale(2, RoundingMode.HALF_UP);
    }

    private static String valor(String[] arreglo, int k, String porDefecto) {
        if (arreglo == null || k >= arreglo.length || !tieneTexto(arreglo[k])) {
            return porDefecto;
        }
        return arreglo[k].trim();
    }
}
