package com.trajes.servicio;

import com.trajes.modelo.HistorialTraje;
import com.trajes.modelo.MantenimientoFila;
import com.trajes.modelo.Opcion;
import com.trajes.modelo.PiezaFila;
import com.trajes.modelo.Traje;
import com.trajes.modelo.TrajeFila;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TrajeService {

    private final JdbcTemplate jdbc;

    public TrajeService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<TrajeFila> TRAJE_FILA = (rs, i) -> new TrajeFila(
            rs.getInt("id_traje"), rs.getString("codigo"), rs.getString("danza"),
            rs.getString("personaje"), rs.getString("genero"), rs.getString("talla"),
            rs.getString("color"), rs.getBigDecimal("precio_alquiler"), rs.getBigDecimal("garantia"),
            rs.getString("estado"), rs.getLong("veces_alquilado"));

    private static final RowMapper<Traje> TRAJE = (rs, i) -> new Traje(
            rs.getInt("id_traje"), rs.getString("codigo"), rs.getInt("id_danza"), rs.getString("danza"),
            rs.getString("personaje"), rs.getString("genero"), rs.getString("talla"), rs.getString("color"),
            rs.getBigDecimal("precio_alquiler"), rs.getBigDecimal("garantia"),
            Filas.fecha(rs, "fecha_adquisicion"), rs.getString("estado"));

    private static final RowMapper<Opcion> OPCION = (rs, i) -> new Opcion(rs.getInt("id"), rs.getString("nombre"));

    public List<TrajeFila> listar(Integer idDanza, String estado, String talla, String texto) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.id_traje, t.codigo, d.nombre AS danza, t.personaje, t.genero, t.talla, t.color,
                       t.precio_alquiler, t.garantia, t.estado,
                       (SELECT COUNT(*) FROM detalle_alquiler da WHERE da.id_traje = t.id_traje) AS veces_alquilado
                FROM traje t
                JOIN danza d ON d.id_danza = t.id_danza
                WHERE 1 = 1
                """);
        List<Object> params = new ArrayList<>();
        if (idDanza != null) {
            sql.append(" AND t.id_danza = ?");
            params.add(idDanza);
        }
        if (tieneTexto(estado)) {
            sql.append(" AND t.estado = ?");
            params.add(estado);
        }
        if (tieneTexto(talla)) {
            sql.append(" AND t.talla = ?");
            params.add(talla);
        }
        if (tieneTexto(texto)) {
            sql.append(" AND (t.codigo ILIKE ? OR t.personaje ILIKE ? OR t.color ILIKE ?)");
            String patron = "%" + texto.trim() + "%";
            params.add(patron);
            params.add(patron);
            params.add(patron);
        }
        sql.append(" ORDER BY d.nombre, t.personaje, t.talla, t.codigo");
        return jdbc.query(sql.toString(), TRAJE_FILA, params.toArray());
    }

    public Traje buscar(int idTraje) {
        List<Traje> lista = jdbc.query("""
                SELECT t.*, d.nombre AS danza
                FROM traje t
                JOIN danza d ON d.id_danza = t.id_danza
                WHERE t.id_traje = ?
                """, TRAJE, idTraje);
        if (lista.isEmpty()) {
            throw new ReglaNegocioException("No existe el traje " + idTraje);
        }
        return lista.get(0);
    }

    public List<Opcion> danzas() {
        return jdbc.query("SELECT id_danza AS id, nombre FROM danza ORDER BY nombre", OPCION);
    }

    public List<Opcion> tiposPieza() {
        return jdbc.query("SELECT id_tipo_pieza AS id, nombre FROM tipo_pieza ORDER BY nombre", OPCION);
    }

    public List<PiezaFila> piezas(int idTraje) {
        return jdbc.query("""
                SELECT p.id_pieza, tp.nombre AS tipo, p.descripcion, p.estado, p.valor_reposicion
                FROM pieza p
                JOIN tipo_pieza tp ON tp.id_tipo_pieza = p.id_tipo_pieza
                WHERE p.id_traje = ?
                ORDER BY tp.nombre
                """, (rs, i) -> new PiezaFila(rs.getInt("id_pieza"), rs.getString("tipo"),
                rs.getString("descripcion"), rs.getString("estado"), rs.getBigDecimal("valor_reposicion")), idTraje);
    }

    public List<HistorialTraje> historial(int idTraje) {
        return jdbc.query("""
                SELECT a.id_alquiler, CONCAT_WS(' ', c.nombres, c.apellidos) AS cliente,
                       a.fecha_salida, a.fecha_devolucion_prevista, a.estado, d.precio
                FROM detalle_alquiler d
                JOIN alquiler a ON a.id_alquiler = d.id_alquiler
                JOIN cliente c  ON c.id_cliente = a.id_cliente
                WHERE d.id_traje = ?
                ORDER BY a.fecha_salida DESC
                """, (rs, i) -> new HistorialTraje(rs.getInt("id_alquiler"), rs.getString("cliente"),
                Filas.fecha(rs, "fecha_salida"), Filas.fecha(rs, "fecha_devolucion_prevista"),
                rs.getString("estado"), rs.getBigDecimal("precio")), idTraje);
    }

    public List<MantenimientoFila> mantenimientos(int idTraje) {
        return jdbc.query("""
                SELECT id_mantenimiento, tipo, fecha_inicio, fecha_fin, costo, detalle
                FROM mantenimiento
                WHERE id_traje = ?
                ORDER BY fecha_inicio DESC
                """, (rs, i) -> new MantenimientoFila(rs.getInt("id_mantenimiento"), rs.getString("tipo"),
                Filas.fecha(rs, "fecha_inicio"), Filas.fecha(rs, "fecha_fin"),
                rs.getBigDecimal("costo"), rs.getString("detalle")), idTraje);
    }

    public int guardar(Integer idTraje, String codigo, int idDanza, String personaje, String genero, String talla,
                       String color, BigDecimal precio, BigDecimal garantia, LocalDate fechaAdquisicion) {
        String codigoLimpio = codigo.trim().toUpperCase();
        if (idTraje == null) {
            return jdbc.queryForObject("""
                    INSERT INTO traje (codigo, id_danza, personaje, genero, talla, color,
                                       precio_alquiler, garantia, fecha_adquisicion)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    RETURNING id_traje
                    """, Integer.class, codigoLimpio, idDanza, personaje.trim(), genero, talla,
                    vacioANulo(color), precio, garantia, fechaAdquisicion);
        }
        jdbc.update("""
                UPDATE traje
                SET codigo = ?, id_danza = ?, personaje = ?, genero = ?, talla = ?, color = ?,
                    precio_alquiler = ?, garantia = ?, fecha_adquisicion = ?
                WHERE id_traje = ?
                """, codigoLimpio, idDanza, personaje.trim(), genero, talla, vacioANulo(color),
                precio, garantia, fechaAdquisicion, idTraje);
        return idTraje;
    }

    public void agregarPieza(int idTraje, int idTipoPieza, String descripcion, BigDecimal valor) {
        jdbc.update("""
                INSERT INTO pieza (id_traje, id_tipo_pieza, descripcion, valor_reposicion)
                VALUES (?, ?, ?, ?)
                """, idTraje, idTipoPieza, vacioANulo(descripcion), valor);
    }

    public void eliminarPieza(int idPieza) {
        jdbc.update("DELETE FROM pieza WHERE id_pieza = ?", idPieza);
    }

    @Transactional
    public void enviarAMantenimiento(int idTraje, String tipo, String detalle) {
        validarSinAlquileresActivos(idTraje, "enviar a mantenimiento");
        jdbc.update("""
                INSERT INTO mantenimiento (id_traje, tipo, fecha_inicio, detalle)
                VALUES (?, ?, CURRENT_DATE, ?)
                """, idTraje, tipo, vacioANulo(detalle));
        jdbc.update("UPDATE traje SET estado = 'MANTENIMIENTO' WHERE id_traje = ?", idTraje);
    }

    @Transactional
    public void terminarMantenimiento(int idTraje, BigDecimal costo) {
        jdbc.update("""
                UPDATE mantenimiento
                SET fecha_fin = CURRENT_DATE, costo = ?
                WHERE id_traje = ? AND fecha_fin IS NULL
                """, costo == null ? BigDecimal.ZERO : costo, idTraje);
        jdbc.update("UPDATE traje SET estado = 'DISPONIBLE' WHERE id_traje = ?", idTraje);
    }

    public void darDeBaja(int idTraje) {
        validarSinAlquileresActivos(idTraje, "dar de baja");
        jdbc.update("UPDATE traje SET estado = 'BAJA' WHERE id_traje = ?", idTraje);
    }

    public void reactivar(int idTraje) {
        jdbc.update("UPDATE traje SET estado = 'DISPONIBLE' WHERE id_traje = ? AND estado = 'BAJA'", idTraje);
    }

    private void validarSinAlquileresActivos(int idTraje, String accion) {
        Integer activos = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM detalle_alquiler d
                JOIN alquiler a ON a.id_alquiler = d.id_alquiler
                WHERE d.id_traje = ? AND a.estado IN ('SOLICITADO', 'RESERVADO', 'ENTREGADO')
                """, Integer.class, idTraje);
        if (activos != null && activos > 0) {
            throw new ReglaNegocioException("No se puede " + accion
                    + ": el traje tiene " + activos + " alquiler(es) solicitado(s), reservado(s) o entregado(s).");
        }
    }

    static boolean tieneTexto(String s) {
        return s != null && !s.isBlank();
    }

    static String vacioANulo(String s) {
        return tieneTexto(s) ? s.trim() : null;
    }
}
