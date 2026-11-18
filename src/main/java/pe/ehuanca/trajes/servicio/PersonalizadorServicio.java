/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.servicio;

import pe.ehuanca.trajes.modelo.ColorTraje;
import pe.ehuanca.trajes.modelo.Implemento;
import pe.ehuanca.trajes.modelo.Opcion;
import pe.ehuanca.trajes.modelo.ParteTraje;
import pe.ehuanca.trajes.modelo.PedidoDetalle;
import pe.ehuanca.trajes.modelo.PedidoImplemento;
import pe.ehuanca.trajes.modelo.PedidoParte;
import pe.ehuanca.trajes.modelo.PedidoResumen;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static pe.ehuanca.trajes.servicio.TrajeServicio.tieneTexto;
import static pe.ehuanca.trajes.servicio.TrajeServicio.vacioANulo;

/**
 * Personalizador de trajes: catálogos (partes, colores, implementos) y pedidos.
 * El monto de cada pedido lo calcula el trigger trg_recalcular_pedido_* de la base de datos.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
@Service
public class PersonalizadorServicio {

    public static final List<String> ESTADOS = List.of("PENDIENTE", "COTIZADO", "APROBADO", "EN_CONFECCION",
            "LISTO", "ENTREGADO", "CANCELADO");
    public static final List<String> TIPOS = List.of("CONFECCION", "ALQUILER");
    public static final BigDecimal FACTOR_ALQUILER = new BigDecimal("0.30");

    private final JdbcTemplate jdbcErht;

    public PersonalizadorServicio(JdbcTemplate jdbc) {
        this.jdbcErht = jdbc;
    }

    private static final RowMapper<ParteTraje> PARTE = (rs, i) -> new ParteTraje(
            rs.getInt("id_parte"), rs.getInt("id_danza"), rs.getString("nombre"), rs.getBigDecimal("precio_base"));

    private static final RowMapper<ColorTraje> COLOR = (rs, i) -> new ColorTraje(
            rs.getInt("id_color"), rs.getString("nombre"), rs.getString("hex"));

    private static final RowMapper<Implemento> IMPLEMENTO = (rs, i) -> new Implemento(
            rs.getInt("id_implemento"), rs.getString("nombre"), rs.getBigDecimal("precio"), rs.getString("parte_destino"));

    private static final RowMapper<PedidoResumen> RESUMEN = (rs, i) -> new PedidoResumen(
            rs.getInt("id_pedido"), rs.getInt("id_cliente"), rs.getString("cliente"),
            rs.getInt("id_danza"), rs.getString("danza"), rs.getString("talla"), rs.getString("tipo"),
            rs.getString("estado"), Filas.fecha(rs, "fecha_pedido"), Filas.fecha(rs, "fecha_entrega"),
            rs.getLong("num_partes"), rs.getLong("num_implementos"), rs.getBigDecimal("monto_total"),
            rs.getBigDecimal("adelanto"), rs.getBigDecimal("saldo"), rs.getString("observacion"),
            rs.getBigDecimal("precio_estimado"));

    public List<Opcion> danzas() {
        return jdbcErht.query("""
                SELECT d.id_danza AS id, d.nombre
                FROM danza d
                WHERE EXISTS (SELECT 1 FROM parte_traje p WHERE p.id_danza = d.id_danza)
                ORDER BY d.nombre
                """, (rs, i) -> new Opcion(rs.getInt("id"), rs.getString("nombre")));
    }

    public List<ParteTraje> partesDe(int idDanza) {
        return jdbcErht.query("""
                SELECT * FROM parte_traje
                WHERE id_danza = ?
                ORDER BY CASE nombre WHEN 'Máscara' THEN 1 WHEN 'Sombrero' THEN 2 WHEN 'Chaqueta' THEN 3
                                     WHEN 'Faja' THEN 4 WHEN 'Capa' THEN 5 WHEN 'Pantalón' THEN 6
                                     WHEN 'Pollera' THEN 7 WHEN 'Botas' THEN 8 ELSE 9 END, id_parte
                """, PARTE, idDanza);
    }

    public List<ColorTraje> colores() {
        return jdbcErht.query("SELECT * FROM color ORDER BY id_color", COLOR);
    }

    public List<Implemento> implementos() {
        return jdbcErht.query("SELECT * FROM implemento ORDER BY nombre", IMPLEMENTO);
    }

    @Transactional
    public int crearPedido(int idCliente, int idDanza, String talla, String tipo, LocalDate fechaEntrega,
                           Map<Integer, Integer> partes, Map<Integer, Integer> implementos,
                           BigDecimal adelanto, String observacion) {
        if (partes == null || partes.isEmpty()) {
            throw new ExcepcionReglaNegocio("Incluye al menos una parte del traje.");
        }
        if (fechaEntrega.isBefore(LocalDate.now())) {
            throw new ExcepcionReglaNegocio("La fecha de entrega no puede ser anterior a hoy.");
        }
        BigDecimal pagadoErht = adelanto == null ? BigDecimal.ZERO : adelanto;
        if (pagadoErht.signum() < 0) {
            throw new ExcepcionReglaNegocio("El adelanto no puede ser negativo.");
        }
        Integer idPedidoErht = jdbcErht.queryForObject("""
                INSERT INTO pedido_personalizado (id_cliente, id_danza, talla, fecha_entrega, tipo, observacion)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id_pedido
                """, Integer.class, idCliente, idDanza, talla, fechaEntrega, tipo, vacioANulo(observacion));

        partes.forEach((idParte, idColor) -> jdbcErht.update(
                "INSERT INTO pedido_parte (id_pedido, id_parte, id_color) VALUES (?, ?, ?)",
                idPedidoErht, idParte, idColor));
        if (implementos != null) {
            implementos.forEach((idImplemento, cantidad) -> jdbcErht.update(
                    "INSERT INTO pedido_implemento (id_pedido, id_implemento, cantidad) VALUES (?, ?, ?)",
                    idPedidoErht, idImplemento, cantidad));
        }

        if (pagadoErht.signum() > 0) {
            BigDecimal totalErht = jdbcErht.queryForObject(
                    "SELECT monto_total FROM pedido_personalizado WHERE id_pedido = ?", BigDecimal.class, idPedidoErht);
            if (pagadoErht.compareTo(totalErht) > 0) {
                throw new ExcepcionReglaNegocio("El adelanto no puede ser mayor que el total del pedido.");
            }
            jdbcErht.update("UPDATE pedido_personalizado SET adelanto = ? WHERE id_pedido = ?", pagadoErht, idPedidoErht);
        }
        return idPedidoErht;
    }

    public List<PedidoResumen> listarPedidos(String estado, String texto) {
        StringBuilder sqlErht = new StringBuilder("SELECT * FROM v_pedidos_resumen WHERE 1 = 1");
        List<Object> parametrosErht = new ArrayList<>();
        if (tieneTexto(estado)) {
            sqlErht.append(" AND estado = ?");
            parametrosErht.add(estado);
        }
        if (tieneTexto(texto)) {
            sqlErht.append(" AND (cliente ILIKE ? OR danza ILIKE ? OR id_pedido::text = ?)");
            String patronErht = "%" + texto.trim() + "%";
            parametrosErht.add(patronErht);
            parametrosErht.add(patronErht);
            parametrosErht.add(texto.trim());
        }
        sqlErht.append(" ORDER BY fecha_pedido DESC, id_pedido DESC");
        return jdbcErht.query(sqlErht.toString(), RESUMEN, parametrosErht.toArray());
    }

    public void cotizar(int idPedido, BigDecimal precio) {
        if (precio == null || precio.signum() <= 0) {
            throw new ExcepcionReglaNegocio("Escribe un precio mayor que cero.");
        }
        PedidoResumen p = buscarPedido(idPedido);
        if (precio.compareTo(p.adelanto()) < 0) {
            throw new ExcepcionReglaNegocio("El precio no puede ser menor que lo ya pagado ("
                    + p.adelanto().setScale(2) + " soles).");
        }
        int n = jdbcErht.update("""
                UPDATE pedido_personalizado SET monto_total = ?, estado = 'COTIZADO'
                WHERE id_pedido = ? AND estado = 'PENDIENTE'
                """, precio.setScale(2, java.math.RoundingMode.HALF_UP), idPedido);
        if (n == 0) {
            throw new ExcepcionReglaNegocio("Solo se cotizan pedidos en estado PENDIENTE (este está " + p.estado() + ").");
        }
    }

    public void guardarImagen(int idPedido, int idCliente, byte[] png) {
        jdbcErht.update("UPDATE pedido_personalizado SET imagen_diseno = ? WHERE id_pedido = ? AND id_cliente = ?",
                png, idPedido, idCliente);
    }

    public byte[] imagenDiseno(int idPedido, int idCliente) {
        List<byte[]> l = jdbcErht.query(
                "SELECT imagen_diseno FROM pedido_personalizado WHERE id_pedido = ? AND id_cliente = ?",
                (rs, i) -> rs.getBytes(1), idPedido, idCliente);
        return l.isEmpty() ? null : l.get(0);
    }

    public void aprobarCotizacion(int idPedido, int idCliente) {
        int n = jdbcErht.update("""
                UPDATE pedido_personalizado SET estado = 'APROBADO'
                WHERE id_pedido = ? AND id_cliente = ? AND estado = 'COTIZADO'
                """, idPedido, idCliente);
        if (n == 0) {
            throw new ExcepcionReglaNegocio("Solo puedes aprobar tus diseños que tienen precio cotizado.");
        }
    }

    public void rechazarCotizacion(int idPedido, int idCliente) {
        int n = jdbcErht.update("""
                UPDATE pedido_personalizado
                SET estado = 'CANCELADO',
                    observacion = LEFT(CONCAT_WS(' · ', observacion, 'El cliente no aceptó la cotización'), 200)
                WHERE id_pedido = ? AND id_cliente = ? AND estado = 'COTIZADO'
                """, idPedido, idCliente);
        if (n == 0) {
            throw new ExcepcionReglaNegocio("Solo puedes rechazar tus diseños que tienen precio cotizado.");
        }
    }

    @Transactional
    public int crearPedidoDiseno(int idCliente, int idDanza, String talla, String tipo, LocalDate fechaEntrega,
                                 List<pe.ehuanca.trajes.modelo.ParteElegida> partes, Map<Integer, Integer> implementos,
                                 String observacion) {
        if (partes == null || partes.isEmpty()) {
            throw new ExcepcionReglaNegocio("Elige el color de al menos una prenda.");
        }
        if (talla == null || talla.isBlank()) {
            throw new ExcepcionReglaNegocio("Elige tu talla.");
        }
        if (fechaEntrega == null || fechaEntrega.isBefore(LocalDate.now().plusDays(1))) {
            throw new ExcepcionReglaNegocio("La fecha de entrega debe ser desde mañana.");
        }
        if (!TIPOS.contains(tipo)) {
            throw new ExcepcionReglaNegocio("Elige la modalidad: confeccionar o alquilar.");
        }
        Integer idPedidoErht = jdbcErht.queryForObject("""
                INSERT INTO pedido_personalizado (id_cliente, id_danza, talla, fecha_entrega, tipo, observacion)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id_pedido
                """, Integer.class, idCliente, idDanza, talla, fechaEntrega, tipo, vacioANulo(observacion));
        for (pe.ehuanca.trajes.modelo.ParteElegida p : partes) {
            String colorHexErht = p.colorHex() == null ? null : p.colorHex().toUpperCase(java.util.Locale.ROOT);
            jdbcErht.update("INSERT INTO pedido_parte (id_pedido, id_parte, id_color, color_hex) VALUES (?, ?, ?, ?)",
                    idPedidoErht, p.idParte(), p.idColor(), colorHexErht);
        }
        if (implementos != null) {
            implementos.forEach((idImplemento, cantidad) -> jdbcErht.update(
                    "INSERT INTO pedido_implemento (id_pedido, id_implemento, cantidad) VALUES (?, ?, ?)",
                    idPedidoErht, idImplemento, cantidad));
        }
        jdbcErht.update("UPDATE pedido_personalizado SET precio_estimado = monto_total WHERE id_pedido = ?", idPedidoErht);
        return idPedidoErht;
    }

    public void guardarRutaImagen(int idPedido, int idCliente, String ruta) {
        jdbcErht.update("UPDATE pedido_personalizado SET imagen_ruta = ? WHERE id_pedido = ? AND id_cliente = ?",
                ruta, idPedido, idCliente);
    }

    public byte[] imagenDisenoTienda(int idPedido) {
        List<byte[]> l = jdbcErht.query("SELECT imagen_diseno FROM pedido_personalizado WHERE id_pedido = ?",
                (rs, i) -> rs.getBytes(1), idPedido);
        return l.isEmpty() ? null : l.get(0);
    }

    public int contarPendientes() {
        Integer n = jdbcErht.queryForObject("SELECT COUNT(*) FROM pedido_personalizado WHERE estado = 'PENDIENTE'",
                Integer.class);
        return n == null ? 0 : n;
    }

    public List<PedidoResumen> pedidosDelCliente(int idCliente) {
        return jdbcErht.query("SELECT * FROM v_pedidos_resumen WHERE id_cliente = ? ORDER BY fecha_pedido DESC, id_pedido DESC",
                RESUMEN, idCliente);
    }

    public PedidoResumen buscarPedido(int idPedido) {
        List<PedidoResumen> listaErht = jdbcErht.query("SELECT * FROM v_pedidos_resumen WHERE id_pedido = ?", RESUMEN, idPedido);
        if (listaErht.isEmpty()) {
            throw new ExcepcionReglaNegocio("No existe el pedido N° " + idPedido + ".");
        }
        return listaErht.get(0);
    }

    public PedidoDetalle detallePedido(int idPedido) {
        PedidoResumen resumenErht = buscarPedido(idPedido);
        List<PedidoParte> partesErht = jdbcErht.query("""
                SELECT pt.id_parte, pt.nombre AS parte, pt.precio_base, c.id_color,
                       COALESCE(c.nombre, 'Personalizado') AS color, COALESCE(pp.color_hex, c.hex) AS hex
                FROM pedido_parte pp
                JOIN parte_traje pt  ON pt.id_parte = pp.id_parte
                LEFT JOIN color c    ON c.id_color = pp.id_color
                WHERE pp.id_pedido = ?
                ORDER BY pt.id_parte
                """, (rs, i) -> new PedidoParte(rs.getInt("id_parte"), rs.getString("parte"),
                rs.getBigDecimal("precio_base"), rs.getInt("id_color"), rs.getString("color"), rs.getString("hex")),
                idPedido);
        List<PedidoImplemento> implementosErht = jdbcErht.query("""
                SELECT i.id_implemento, i.nombre, i.parte_destino, i.precio, pi.cantidad,
                       i.precio * pi.cantidad AS subtotal
                FROM pedido_implemento pi
                JOIN implemento i ON i.id_implemento = pi.id_implemento
                WHERE pi.id_pedido = ?
                ORDER BY i.nombre
                """, (rs, i) -> new PedidoImplemento(rs.getInt("id_implemento"), rs.getString("nombre"),
                rs.getString("parte_destino"), rs.getBigDecimal("precio"), rs.getInt("cantidad"),
                rs.getBigDecimal("subtotal")), idPedido);
        return new PedidoDetalle(resumenErht, partesErht, implementosErht);
    }

    public static String siguienteEstado(String estado) {
        return switch (estado) {
            case "PENDIENTE" -> "COTIZADO";
            case "COTIZADO" -> "APROBADO";
            case "APROBADO" -> "EN_CONFECCION";
            case "EN_CONFECCION" -> "LISTO";
            case "LISTO" -> "ENTREGADO";
            default -> null;
        };
    }

    @Transactional
    public void cambiarEstado(int idPedido, String estado) {
        PedidoResumen p = buscarPedido(idPedido);
        if ("CANCELADO".equals(estado)) {
            if ("ENTREGADO".equals(p.estado()) || "CANCELADO".equals(p.estado())) {
                throw new ExcepcionReglaNegocio("No se puede cancelar un pedido " + p.estado() + ".");
            }
        } else if (!estado.equals(siguienteEstado(p.estado()))) {
            throw new ExcepcionReglaNegocio("El pedido está " + p.estado() + " y no puede pasar a " + estado + ".");
        }
        if ("ENTREGADO".equals(estado) && p.saldo().signum() > 0) {
            throw new ExcepcionReglaNegocio("Falta pagar " + p.saldo().setScale(2) + " soles. "
                    + "Registra el pago del saldo antes de entregar el pedido.");
        }
        jdbcErht.update("UPDATE pedido_personalizado SET estado = ? WHERE id_pedido = ?", estado, idPedido);
    }

    @Transactional
    public void registrarAdelanto(int idPedido, BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new ExcepcionReglaNegocio("El monto del adelanto debe ser mayor que cero.");
        }
        PedidoResumen p = buscarPedido(idPedido);
        if ("CANCELADO".equals(p.estado()) || "ENTREGADO".equals(p.estado())) {
            throw new ExcepcionReglaNegocio("El pedido está " + p.estado() + ": ya no recibe pagos.");
        }
        if (monto.compareTo(p.saldo()) > 0) {
            throw new ExcepcionReglaNegocio("El monto supera el saldo pendiente (" + p.saldo().setScale(2) + " soles).");
        }
        jdbcErht.update("UPDATE pedido_personalizado SET adelanto = adelanto + ? WHERE id_pedido = ?", monto, idPedido);
    }
}
