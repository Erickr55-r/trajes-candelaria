package com.trajes.servicio;

import com.trajes.modelo.ColorTraje;
import com.trajes.modelo.Implemento;
import com.trajes.modelo.Opcion;
import com.trajes.modelo.ParteTraje;
import com.trajes.modelo.PedidoDetalle;
import com.trajes.modelo.PedidoImplemento;
import com.trajes.modelo.PedidoParte;
import com.trajes.modelo.PedidoResumen;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.trajes.servicio.TrajeService.tieneTexto;
import static com.trajes.servicio.TrajeService.vacioANulo;

/**
 * Personalizador de trajes: catálogos (partes, colores, implementos) y pedidos.
 * El monto de cada pedido lo calcula el trigger trg_recalcular_pedido_* de la base de datos.
 */
@Service
public class PersonalizadorService {

    public static final List<String> ESTADOS = List.of("PENDIENTE", "COTIZADO", "APROBADO", "EN_CONFECCION",
            "LISTO", "ENTREGADO", "CANCELADO");
    public static final List<String> TIPOS = List.of("CONFECCION", "ALQUILER");
    /** Mismo factor que el trigger: un ALQUILER paga el 30% del costo de confección. */
    public static final BigDecimal FACTOR_ALQUILER = new BigDecimal("0.30");

    private final JdbcTemplate jdbc;

    public PersonalizadorService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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

    // ------------------------------------------------------------------ catálogos

    /** Danzas que tienen partes configuradas en el personalizador. */
    public List<Opcion> danzas() {
        return jdbc.query("""
                SELECT d.id_danza AS id, d.nombre
                FROM danza d
                WHERE EXISTS (SELECT 1 FROM parte_traje p WHERE p.id_danza = d.id_danza)
                ORDER BY d.nombre
                """, (rs, i) -> new Opcion(rs.getInt("id"), rs.getString("nombre")));
    }

    public List<ParteTraje> partesDe(int idDanza) {
        // De la cabeza a los pies, como se ven en el maniquí
        return jdbc.query("""
                SELECT * FROM parte_traje
                WHERE id_danza = ?
                ORDER BY CASE nombre WHEN 'Máscara' THEN 1 WHEN 'Sombrero' THEN 2 WHEN 'Chaqueta' THEN 3
                                     WHEN 'Faja' THEN 4 WHEN 'Capa' THEN 5 WHEN 'Pantalón' THEN 6
                                     WHEN 'Pollera' THEN 7 WHEN 'Botas' THEN 8 ELSE 9 END, id_parte
                """, PARTE, idDanza);
    }

    public List<ColorTraje> colores() {
        return jdbc.query("SELECT * FROM color ORDER BY id_color", COLOR);
    }

    public List<Implemento> implementos() {
        return jdbc.query("SELECT * FROM implemento ORDER BY nombre", IMPLEMENTO);
    }

    // ------------------------------------------------------------------ pedidos

    /**
     * Registra un pedido con el color de cada parte y sus implementos. Si algo falla
     * (por ejemplo, una parte de otra danza o un adelanto mayor al total), se deshace todo.
     *
     * @param partes       id de parte → id de color
     * @param implementos  id de implemento → cantidad
     */
    @Transactional
    public int crearPedido(int idCliente, int idDanza, String talla, String tipo, LocalDate fechaEntrega,
                           Map<Integer, Integer> partes, Map<Integer, Integer> implementos,
                           BigDecimal adelanto, String observacion) {
        if (partes == null || partes.isEmpty()) {
            throw new ReglaNegocioException("Incluye al menos una parte del traje.");
        }
        if (fechaEntrega.isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de entrega no puede ser anterior a hoy.");
        }
        BigDecimal pagado = adelanto == null ? BigDecimal.ZERO : adelanto;
        if (pagado.signum() < 0) {
            throw new ReglaNegocioException("El adelanto no puede ser negativo.");
        }
        Integer idPedido = jdbc.queryForObject("""
                INSERT INTO pedido_personalizado (id_cliente, id_danza, talla, fecha_entrega, tipo, observacion)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id_pedido
                """, Integer.class, idCliente, idDanza, talla, fechaEntrega, tipo, vacioANulo(observacion));

        partes.forEach((idParte, idColor) -> jdbc.update(
                "INSERT INTO pedido_parte (id_pedido, id_parte, id_color) VALUES (?, ?, ?)",
                idPedido, idParte, idColor));
        if (implementos != null) {
            implementos.forEach((idImplemento, cantidad) -> jdbc.update(
                    "INSERT INTO pedido_implemento (id_pedido, id_implemento, cantidad) VALUES (?, ?, ?)",
                    idPedido, idImplemento, cantidad));
        }

        if (pagado.signum() > 0) {
            BigDecimal total = jdbc.queryForObject(
                    "SELECT monto_total FROM pedido_personalizado WHERE id_pedido = ?", BigDecimal.class, idPedido);
            if (pagado.compareTo(total) > 0) {
                throw new ReglaNegocioException("El adelanto no puede ser mayor que el total del pedido.");
            }
            jdbc.update("UPDATE pedido_personalizado SET adelanto = ? WHERE id_pedido = ?", pagado, idPedido);
        }
        return idPedido;
    }

    public List<PedidoResumen> listarPedidos(String estado, String texto) {
        StringBuilder sql = new StringBuilder("SELECT * FROM v_pedidos_resumen WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (tieneTexto(estado)) {
            sql.append(" AND estado = ?");
            params.add(estado);
        }
        if (tieneTexto(texto)) {
            sql.append(" AND (cliente ILIKE ? OR danza ILIKE ? OR id_pedido::text = ?)");
            String patron = "%" + texto.trim() + "%";
            params.add(patron);
            params.add(patron);
            params.add(texto.trim());
        }
        sql.append(" ORDER BY fecha_pedido DESC, id_pedido DESC");
        return jdbc.query(sql.toString(), RESUMEN, params.toArray());
    }

    /**
     * La tienda cotiza un pedido PENDIENTE: fija el precio final (puede ser distinto del calculado por las
     * partes) y lo pasa a COTIZADO para que el cliente lo apruebe. El trigger solo recalcula el monto cuando
     * cambian las partes o los implementos, y estos ya no se tocan después de crear el pedido.
     */
    public void cotizar(int idPedido, BigDecimal precio) {
        if (precio == null || precio.signum() <= 0) {
            throw new ReglaNegocioException("Escribe un precio mayor que cero.");
        }
        PedidoResumen p = buscarPedido(idPedido);
        if (precio.compareTo(p.adelanto()) < 0) {
            throw new ReglaNegocioException("El precio no puede ser menor que lo ya pagado ("
                    + p.adelanto().setScale(2) + " soles).");
        }
        int n = jdbc.update("""
                UPDATE pedido_personalizado SET monto_total = ?, estado = 'COTIZADO'
                WHERE id_pedido = ? AND estado = 'PENDIENTE'
                """, precio.setScale(2, java.math.RoundingMode.HALF_UP), idPedido);
        if (n == 0) {
            throw new ReglaNegocioException("Solo se cotizan pedidos en estado PENDIENTE (este está " + p.estado() + ").");
        }
    }

    /** Guarda la imagen PNG del diseño, solo si el pedido es del cliente. */
    public void guardarImagen(int idPedido, int idCliente, byte[] png) {
        jdbc.update("UPDATE pedido_personalizado SET imagen_diseno = ? WHERE id_pedido = ? AND id_cliente = ?",
                png, idPedido, idCliente);
    }

    /** Imagen del diseño de un pedido del cliente (null si no tiene). */
    public byte[] imagenDiseno(int idPedido, int idCliente) {
        List<byte[]> l = jdbc.query(
                "SELECT imagen_diseno FROM pedido_personalizado WHERE id_pedido = ? AND id_cliente = ?",
                (rs, i) -> rs.getBytes(1), idPedido, idCliente);
        return l.isEmpty() ? null : l.get(0);
    }

    /** El cliente acepta el precio cotizado: COTIZADO → APROBADO. */
    public void aprobarCotizacion(int idPedido, int idCliente) {
        int n = jdbc.update("""
                UPDATE pedido_personalizado SET estado = 'APROBADO'
                WHERE id_pedido = ? AND id_cliente = ? AND estado = 'COTIZADO'
                """, idPedido, idCliente);
        if (n == 0) {
            throw new ReglaNegocioException("Solo puedes aprobar tus diseños que tienen precio cotizado.");
        }
    }

    /** El cliente no acepta el precio: el pedido queda CANCELADO. */
    public void rechazarCotizacion(int idPedido, int idCliente) {
        int n = jdbc.update("""
                UPDATE pedido_personalizado
                SET estado = 'CANCELADO',
                    observacion = LEFT(CONCAT_WS(' · ', observacion, 'El cliente no aceptó la cotización'), 200)
                WHERE id_pedido = ? AND id_cliente = ? AND estado = 'COTIZADO'
                """, idPedido, idCliente);
        if (n == 0) {
            throw new ReglaNegocioException("Solo puedes rechazar tus diseños que tienen precio cotizado.");
        }
    }

    /**
     * Pedido hecho con el personalizador por capas: cada prenda guarda su color exacto (hex) y, si es de la
     * paleta, también su id_color. El trigger calcula monto_total con la misma fórmula que el portal muestra
     * como estimado, y ese valor queda guardado en precio_estimado (la cotización cambia monto_total).
     */
    @Transactional
    public int crearPedidoDiseno(int idCliente, int idDanza, String talla, String tipo, LocalDate fechaEntrega,
                                 List<com.trajes.modelo.ParteElegida> partes, Map<Integer, Integer> implementos,
                                 String observacion) {
        if (partes == null || partes.isEmpty()) {
            throw new ReglaNegocioException("Elige el color de al menos una prenda.");
        }
        if (talla == null || talla.isBlank()) {
            throw new ReglaNegocioException("Elige tu talla.");
        }
        if (fechaEntrega == null || fechaEntrega.isBefore(LocalDate.now().plusDays(1))) {
            throw new ReglaNegocioException("La fecha de entrega debe ser desde mañana.");
        }
        if (!TIPOS.contains(tipo)) {
            throw new ReglaNegocioException("Elige la modalidad: confeccionar o alquilar.");
        }
        Integer idPedido = jdbc.queryForObject("""
                INSERT INTO pedido_personalizado (id_cliente, id_danza, talla, fecha_entrega, tipo, observacion)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id_pedido
                """, Integer.class, idCliente, idDanza, talla, fechaEntrega, tipo, vacioANulo(observacion));
        for (com.trajes.modelo.ParteElegida p : partes) {
            String hex = p.hex() == null ? null : p.hex().toUpperCase(java.util.Locale.ROOT);
            jdbc.update("INSERT INTO pedido_parte (id_pedido, id_parte, id_color, color_hex) VALUES (?, ?, ?, ?)",
                    idPedido, p.idParte(), p.idColor(), hex);
        }
        if (implementos != null) {
            implementos.forEach((idImplemento, cantidad) -> jdbc.update(
                    "INSERT INTO pedido_implemento (id_pedido, id_implemento, cantidad) VALUES (?, ?, ?)",
                    idPedido, idImplemento, cantidad));
        }
        jdbc.update("UPDATE pedido_personalizado SET precio_estimado = monto_total WHERE id_pedido = ?", idPedido);
        return idPedido;
    }

    /** Guarda dónde quedó el PNG del diseño (carpeta disenos/). */
    public void guardarRutaImagen(int idPedido, int idCliente, String ruta) {
        jdbc.update("UPDATE pedido_personalizado SET imagen_ruta = ? WHERE id_pedido = ? AND id_cliente = ?",
                ruta, idPedido, idCliente);
    }

    /** Imagen del diseño para la tienda (sin filtrar por cliente: solo la usa el panel del vendedor). */
    public byte[] imagenDisenoTienda(int idPedido) {
        List<byte[]> l = jdbc.query("SELECT imagen_diseno FROM pedido_personalizado WHERE id_pedido = ?",
                (rs, i) -> rs.getBytes(1), idPedido);
        return l.isEmpty() ? null : l.get(0);
    }

    /** Pedidos de diseño que esperan cotización (insignia del panel). */
    public int contarPendientes() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pedido_personalizado WHERE estado = 'PENDIENTE'",
                Integer.class);
        return n == null ? 0 : n;
    }

    /** Pedidos de un cliente (portal del cliente). */
    public List<PedidoResumen> pedidosDelCliente(int idCliente) {
        return jdbc.query("SELECT * FROM v_pedidos_resumen WHERE id_cliente = ? ORDER BY fecha_pedido DESC, id_pedido DESC",
                RESUMEN, idCliente);
    }

    public PedidoResumen buscarPedido(int idPedido) {
        List<PedidoResumen> lista = jdbc.query("SELECT * FROM v_pedidos_resumen WHERE id_pedido = ?", RESUMEN, idPedido);
        if (lista.isEmpty()) {
            throw new ReglaNegocioException("No existe el pedido N° " + idPedido + ".");
        }
        return lista.get(0);
    }

    public PedidoDetalle detallePedido(int idPedido) {
        PedidoResumen resumen = buscarPedido(idPedido);
        List<PedidoParte> partes = jdbc.query("""
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
        List<PedidoImplemento> implementos = jdbc.query("""
                SELECT i.id_implemento, i.nombre, i.parte_destino, i.precio, pi.cantidad,
                       i.precio * pi.cantidad AS subtotal
                FROM pedido_implemento pi
                JOIN implemento i ON i.id_implemento = pi.id_implemento
                WHERE pi.id_pedido = ?
                ORDER BY i.nombre
                """, (rs, i) -> new PedidoImplemento(rs.getInt("id_implemento"), rs.getString("nombre"),
                rs.getString("parte_destino"), rs.getBigDecimal("precio"), rs.getInt("cantidad"),
                rs.getBigDecimal("subtotal")), idPedido);
        return new PedidoDetalle(resumen, partes, implementos);
    }

    /** Estado que sigue en el flujo PENDIENTE → COTIZADO → APROBADO → EN_CONFECCION → LISTO → ENTREGADO. */
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

    /** Avanza al siguiente estado o cancela. Para entregar, el pedido debe estar pagado. */
    @Transactional
    public void cambiarEstado(int idPedido, String estado) {
        PedidoResumen p = buscarPedido(idPedido);
        if ("CANCELADO".equals(estado)) {
            if ("ENTREGADO".equals(p.estado()) || "CANCELADO".equals(p.estado())) {
                throw new ReglaNegocioException("No se puede cancelar un pedido " + p.estado() + ".");
            }
        } else if (!estado.equals(siguienteEstado(p.estado()))) {
            throw new ReglaNegocioException("El pedido está " + p.estado() + " y no puede pasar a " + estado + ".");
        }
        if ("ENTREGADO".equals(estado) && p.saldo().signum() > 0) {
            throw new ReglaNegocioException("Falta pagar " + p.saldo().setScale(2) + " soles. "
                    + "Registra el pago del saldo antes de entregar el pedido.");
        }
        jdbc.update("UPDATE pedido_personalizado SET estado = ? WHERE id_pedido = ?", estado, idPedido);
    }

    /** Suma un pago al adelanto del pedido (sin pasar del total). */
    @Transactional
    public void registrarAdelanto(int idPedido, BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaNegocioException("El monto del adelanto debe ser mayor que cero.");
        }
        PedidoResumen p = buscarPedido(idPedido);
        if ("CANCELADO".equals(p.estado()) || "ENTREGADO".equals(p.estado())) {
            throw new ReglaNegocioException("El pedido está " + p.estado() + ": ya no recibe pagos.");
        }
        if (monto.compareTo(p.saldo()) > 0) {
            throw new ReglaNegocioException("El monto supera el saldo pendiente (" + p.saldo().setScale(2) + " soles).");
        }
        jdbc.update("UPDATE pedido_personalizado SET adelanto = adelanto + ? WHERE id_pedido = ?", monto, idPedido);
    }
}
