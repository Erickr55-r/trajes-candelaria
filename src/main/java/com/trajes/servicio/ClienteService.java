package com.trajes.servicio;

import com.trajes.modelo.Cliente;
import com.trajes.modelo.ClienteFila;
import com.trajes.modelo.ConjuntoFila;
import com.trajes.modelo.Opcion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.trajes.servicio.TrajeService.tieneTexto;
import static com.trajes.servicio.TrajeService.vacioANulo;

@Service
public class ClienteService {

    private final JdbcTemplate jdbc;

    public ClienteService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Cliente> CLIENTE = (rs, i) -> new Cliente(
            rs.getInt("id_cliente"), rs.getString("tipo_documento"), rs.getString("numero_documento"),
            rs.getString("nombres"), rs.getString("apellidos"), rs.getString("telefono"),
            rs.getString("email"), rs.getString("direccion"), Filas.fecha(rs, "fecha_registro"));

    private static final RowMapper<Opcion> OPCION = (rs, i) -> new Opcion(rs.getInt("id"), rs.getString("nombre"));

    public List<ClienteFila> listar(String texto) {
        StringBuilder sql = new StringBuilder("""
                SELECT c.id_cliente, c.tipo_documento, c.numero_documento,
                       CONCAT_WS(' ', c.nombres, c.apellidos) AS nombre_completo,
                       c.telefono, c.email,
                       COUNT(a.id_alquiler) AS alquileres,
                       COALESCE(SUM(a.monto_alquiler) FILTER (WHERE a.estado NOT IN ('ANULADO', 'SOLICITADO', 'RECHAZADO')), 0) AS total_gastado
                FROM cliente c
                LEFT JOIN alquiler a ON a.id_cliente = c.id_cliente
                """);
        List<Object> params = new ArrayList<>();
        if (tieneTexto(texto)) {
            sql.append(" WHERE c.numero_documento ILIKE ? OR CONCAT_WS(' ', c.nombres, c.apellidos) ILIKE ?");
            String patron = "%" + texto.trim() + "%";
            params.add(patron);
            params.add(patron);
        }
        sql.append(" GROUP BY c.id_cliente ORDER BY nombre_completo");
        return jdbc.query(sql.toString(), (rs, i) -> new ClienteFila(rs.getInt("id_cliente"),
                rs.getString("tipo_documento"), rs.getString("numero_documento"), rs.getString("nombre_completo"),
                rs.getString("telefono"), rs.getString("email"), rs.getLong("alquileres"),
                rs.getBigDecimal("total_gastado")), params.toArray());
    }

    public Cliente buscar(int idCliente) {
        List<Cliente> lista = jdbc.query("SELECT * FROM cliente WHERE id_cliente = ?", CLIENTE, idCliente);
        if (lista.isEmpty()) {
            throw new ReglaNegocioException("No existe el cliente " + idCliente);
        }
        return lista.get(0);
    }

    public int guardar(Integer idCliente, String tipoDocumento, String numeroDocumento, String nombres,
                       String apellidos, String telefono, String email, String direccion) {
        if ("DNI".equals(tipoDocumento) && !numeroDocumento.trim().matches("\\d{8}")) {
            throw new ReglaNegocioException("El DNI debe tener 8 dígitos.");
        }
        if ("RUC".equals(tipoDocumento) && !numeroDocumento.trim().matches("\\d{11}")) {
            throw new ReglaNegocioException("El RUC debe tener 11 dígitos.");
        }
        if (idCliente == null) {
            return jdbc.queryForObject("""
                    INSERT INTO cliente (tipo_documento, numero_documento, nombres, apellidos,
                                         telefono, email, direccion)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    RETURNING id_cliente
                    """, Integer.class, tipoDocumento, numeroDocumento.trim(), nombres.trim(),
                    vacioANulo(apellidos), vacioANulo(telefono), vacioANulo(email), vacioANulo(direccion));
        }
        jdbc.update("""
                UPDATE cliente
                SET tipo_documento = ?, numero_documento = ?, nombres = ?, apellidos = ?,
                    telefono = ?, email = ?, direccion = ?
                WHERE id_cliente = ?
                """, tipoDocumento, numeroDocumento.trim(), nombres.trim(), vacioANulo(apellidos),
                vacioANulo(telefono), vacioANulo(email), vacioANulo(direccion), idCliente);
        return idCliente;
    }

    public List<Opcion> opciones() {
        return jdbc.query("""
                SELECT id_cliente AS id,
                       CONCAT_WS(' ', nombres, apellidos) || ' (' || numero_documento || ')' AS nombre
                FROM cliente
                ORDER BY nombres, apellidos
                """, OPCION);
    }

    public List<ConjuntoFila> conjuntos() {
        return jdbc.query("""
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
        jdbc.update("""
                INSERT INTO conjunto (nombre, id_danza, id_delegado, telefono)
                VALUES (?, ?, ?, ?)
                """, nombre.trim(), idDanza, idDelegado, vacioANulo(telefono));
    }

    public List<Opcion> conjuntosDelDelegado(int idCliente) {
        return jdbc.query("SELECT id_conjunto AS id, nombre FROM conjunto WHERE id_delegado = ? ORDER BY nombre",
                OPCION, idCliente);
    }

    public void actualizarPerfil(int idCliente, String nombres, String apellidos, String telefono, String email,
                                 String direccion) {
        if (nombres == null || nombres.isBlank()) {
            throw new ReglaNegocioException("Escribe tus nombres.");
        }
        String tel = telefono == null ? "" : telefono.replace(" ", "").trim();
        if (!tel.isEmpty() && !tel.matches("\\d{6,9}")) {
            throw new ReglaNegocioException("El celular debe tener de 6 a 9 dígitos.");
        }
        String mail = email == null ? "" : email.trim();
        if (!mail.isEmpty() && !mail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new ReglaNegocioException("El correo no es válido.");
        }
        jdbc.update("""
                UPDATE cliente SET nombres = ?, apellidos = ?, telefono = ?, email = ?, direccion = ?
                WHERE id_cliente = ?
                """, nombres.trim(), vacioANulo(apellidos), vacioANulo(tel), vacioANulo(mail), vacioANulo(direccion),
                idCliente);
    }

    public List<Opcion> opcionesConjunto() {
        return jdbc.query("SELECT id_conjunto AS id, nombre FROM conjunto ORDER BY nombre", OPCION);
    }
}
