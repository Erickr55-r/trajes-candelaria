/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.servicio;

import pe.ehuanca.trajes.modelo.Cliente;
import pe.ehuanca.trajes.modelo.ClienteFila;
import pe.ehuanca.trajes.modelo.ConjuntoFila;
import pe.ehuanca.trajes.modelo.Opcion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static pe.ehuanca.trajes.servicio.TrajeServicio.tieneTexto;
import static pe.ehuanca.trajes.servicio.TrajeServicio.vacioANulo;

/**
 * Clientes y conjuntos de danza.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
@Service
public class ClienteServicio {

    private final JdbcTemplate jdbcErht;

    public ClienteServicio(JdbcTemplate jdbc) {
        this.jdbcErht = jdbc;
    }

    private static final RowMapper<Cliente> CLIENTE = (rs, i) -> new Cliente(
            rs.getInt("id_cliente"), rs.getString("tipo_documento"), rs.getString("numero_documento"),
            rs.getString("nombres"), rs.getString("apellidos"), rs.getString("telefono"),
            rs.getString("email"), rs.getString("direccion"), Filas.fecha(rs, "fecha_registro"));

    private static final RowMapper<Opcion> OPCION = (rs, i) -> new Opcion(rs.getInt("id"), rs.getString("nombre"));

    // ------------------------------------------------------------------ clientes

    public List<ClienteFila> listar(String texto) {
        StringBuilder sqlErht = new StringBuilder("""
                SELECT c.id_cliente, c.tipo_documento, c.numero_documento,
                       CONCAT_WS(' ', c.nombres, c.apellidos) AS nombre_completo,
                       c.telefono, c.email,
                       COUNT(a.id_alquiler) AS alquileres,
                       COALESCE(SUM(a.monto_alquiler) FILTER (WHERE a.estado NOT IN ('ANULADO', 'SOLICITADO', 'RECHAZADO')), 0) AS total_gastado
                FROM cliente c
                LEFT JOIN alquiler a ON a.id_cliente = c.id_cliente
                """);
        List<Object> parametrosErht = new ArrayList<>();
        if (tieneTexto(texto)) {
            sqlErht.append(" WHERE c.numero_documento ILIKE ? OR CONCAT_WS(' ', c.nombres, c.apellidos) ILIKE ?");
            String patronErht = "%" + texto.trim() + "%";
            parametrosErht.add(patronErht);
            parametrosErht.add(patronErht);
        }
        sqlErht.append(" GROUP BY c.id_cliente ORDER BY nombre_completo");
        return jdbcErht.query(sqlErht.toString(), (rs, i) -> new ClienteFila(rs.getInt("id_cliente"),
                rs.getString("tipo_documento"), rs.getString("numero_documento"), rs.getString("nombre_completo"),
                rs.getString("telefono"), rs.getString("email"), rs.getLong("alquileres"),
                rs.getBigDecimal("total_gastado")), parametrosErht.toArray());
    }

    public Cliente buscar(int idCliente) {
        List<Cliente> listaErht = jdbcErht.query("SELECT * FROM cliente WHERE id_cliente = ?", CLIENTE, idCliente);
        if (listaErht.isEmpty()) {
            throw new ExcepcionReglaNegocio("No existe el cliente " + idCliente);
        }
        return listaErht.get(0);
    }

    /** Crea un cliente (idCliente null) o actualiza uno existente. Devuelve su id. */
    public int guardar(Integer idCliente, String tipoDocumento, String numeroDocumento, String nombres,
                       String apellidos, String telefono, String correo, String direccion) {
        if ("DNI".equals(tipoDocumento) && !numeroDocumento.trim().matches("\\d{8}")) {
            throw new ExcepcionReglaNegocio("El DNI debe tener 8 dígitos.");
        }
        if ("RUC".equals(tipoDocumento) && !numeroDocumento.trim().matches("\\d{11}")) {
            throw new ExcepcionReglaNegocio("El RUC debe tener 11 dígitos.");
        }
        if (idCliente == null) {
            return jdbcErht.queryForObject("""
                    INSERT INTO cliente (tipo_documento, numero_documento, nombres, apellidos,
                                         telefono, email, direccion)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    RETURNING id_cliente
                    """, Integer.class, tipoDocumento, numeroDocumento.trim(), nombres.trim(),
                    vacioANulo(apellidos), vacioANulo(telefono), vacioANulo(correo), vacioANulo(direccion));
        }
        jdbcErht.update("""
                UPDATE cliente
                SET tipo_documento = ?, numero_documento = ?, nombres = ?, apellidos = ?,
                    telefono = ?, email = ?, direccion = ?
                WHERE id_cliente = ?
                """, tipoDocumento, numeroDocumento.trim(), nombres.trim(), vacioANulo(apellidos),
                vacioANulo(telefono), vacioANulo(correo), vacioANulo(direccion), idCliente);
        return idCliente;
    }

    public List<Opcion> opciones() {
        return jdbcErht.query("""
                SELECT id_cliente AS id,
                       CONCAT_WS(' ', nombres, apellidos) || ' (' || numero_documento || ')' AS nombre
                FROM cliente
                ORDER BY nombres, apellidos
                """, OPCION);
    }

    // ------------------------------------------------------------------ conjuntos

    public List<ConjuntoFila> conjuntos() {
        return jdbcErht.query("""
                SELECT cj.id_conjunto, cj.nombre, d.nombre AS danza,
                       CONCAT_WS(' ', c.nombres, c.apellidos) AS delegado,
                       cj.telefono,
                       (SELECT COUNT(*) FROM alquiler a
                         WHERE a.id_conjunto = cj.id_conjunto
                           AND a.estado NOT IN ('ANULADO', 'SOLICITADO', 'RECHAZADO')) AS alquileres
                FROM conjunto cj
                JOIN danza d        ON d.id_danza = cj.id_danza
                LEFT JOIN cliente c ON c.id_cliente = cj.id_delegado
                ORDER BY cj.nombre
                """, (rs, i) -> new ConjuntoFila(rs.getInt("id_conjunto"), rs.getString("nombre"),
                rs.getString("danza"), rs.getString("delegado"), rs.getString("telefono"), rs.getLong("alquileres")));
    }

    public void crearConjunto(String nombre, int idDanza, Integer idDelegado, String telefono) {
        jdbcErht.update("""
                INSERT INTO conjunto (nombre, id_danza, id_delegado, telefono)
                VALUES (?, ?, ?, ?)
                """, nombre.trim(), idDanza, idDelegado, vacioANulo(telefono));
    }

    /** Conjuntos de los que el cliente es delegado (puede reservar a su nombre desde el portal). */
    public List<Opcion> conjuntosDelDelegado(int idCliente) {
        return jdbcErht.query("SELECT id_conjunto AS id, nombre FROM conjunto WHERE id_delegado = ? ORDER BY nombre",
                OPCION, idCliente);
    }

    /** El cliente actualiza sus datos de contacto desde su perfil (el documento no se cambia aquí). */
    public void actualizarPerfil(int idCliente, String nombres, String apellidos, String telefono, String correo,
                                 String direccion) {
        if (nombres == null || nombres.isBlank()) {
            throw new ExcepcionReglaNegocio("Escribe tus nombres.");
        }
        String telErht = telefono == null ? "" : telefono.replace(" ", "").trim();
        if (!telErht.isEmpty() && !telErht.matches("\\d{6,9}")) {
            throw new ExcepcionReglaNegocio("El celular debe tener de 6 a 9 dígitos.");
        }
        String correoLimpioErht = correo == null ? "" : correo.trim();
        if (!correoLimpioErht.isEmpty() && !correoLimpioErht.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new ExcepcionReglaNegocio("El correo no es válido.");
        }
        jdbcErht.update("""
                UPDATE cliente SET nombres = ?, apellidos = ?, telefono = ?, email = ?, direccion = ?
                WHERE id_cliente = ?
                """, nombres.trim(), vacioANulo(apellidos), vacioANulo(telErht), vacioANulo(correoLimpioErht), vacioANulo(direccion),
                idCliente);
    }

    public List<Opcion> opcionesConjunto() {
        return jdbcErht.query("SELECT id_conjunto AS id, nombre FROM conjunto ORDER BY nombre", OPCION);
    }
}
