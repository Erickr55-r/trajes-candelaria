/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.servicio;

import pe.ehuanca.trajes.modelo.Usuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static pe.ehuanca.trajes.servicio.TrajeServicio.tieneTexto;

/**
 * Inicio de sesión (contraseñas BCrypt) y registro de cuentas de cliente.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
@Service
public class UsuarioServicio {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();
    /** Hash de relleno: si el usuario no existe se compara igual, para no delatarlo por el tiempo de respuesta. */
    private static final String CLAVE_CIFRADA_RELLENO = BCRYPT.encode("relleno-sin-uso");
    private static final String CREDENCIALES_MAL = "Usuario o contraseña incorrectos.";

    private final JdbcTemplate jdbcErht;

    public UsuarioServicio(JdbcTemplate jdbc) {
        this.jdbcErht = jdbc;
    }

    /** Devuelve el usuario si el nombre y la contraseña son correctos y la cuenta está activa. */
    public Usuario autenticar(String nombreUsuario, String clave) {
        if (!tieneTexto(nombreUsuario) || clave == null || clave.isEmpty()) {
            throw new ExcepcionReglaNegocio("Escribe tu usuario y tu contraseña.");
        }
        List<Map<String, Object>> filasErht = jdbcErht.queryForList("""
                SELECT u.id_usuario, u.username, u.password_hash, u.rol, u.id_cliente, u.activo,
                       CONCAT_WS(' ', c.nombres, c.apellidos) AS nombre
                FROM usuario u
                LEFT JOIN cliente c ON c.id_cliente = u.id_cliente
                WHERE LOWER(u.username) = LOWER(?)
                """, nombreUsuario.trim());
        if (filasErht.isEmpty()) {
            BCRYPT.matches(clave, CLAVE_CIFRADA_RELLENO);
            throw new ExcepcionReglaNegocio(CREDENCIALES_MAL);
        }
        Map<String, Object> f = filasErht.get(0);
        if (!BCRYPT.matches(clave, (String) f.get("password_hash"))) {
            throw new ExcepcionReglaNegocio(CREDENCIALES_MAL);
        }
        if (!Boolean.TRUE.equals(f.get("activo"))) {
            throw new ExcepcionReglaNegocio("Tu cuenta está desactivada. Comunícate con la tienda.");
        }
        String nombreErht = (String) f.get("nombre");
        return new Usuario((Integer) f.get("id_usuario"), (String) f.get("username"), (String) f.get("rol"),
                (Integer) f.get("id_cliente"), nombreErht == null || nombreErht.isBlank() ? null : nombreErht);
    }

    /**
     * Crea el cliente y su usuario CLIENTE en una sola transacción (si algo falla, no queda nada a medias)
     * y devuelve el usuario ya autenticado.
     */
    @Transactional
    public Usuario registrarCliente(String nombre, String dni, String celular, String correo,
                                    String nombreUsuario, String clave) {
        String n = limpiarErht(nombre);
        String d = limpiarErht(dni);
        String celErht = limpiarErht(celular).replace(" ", "");
        String correoLimpioErht = limpiarErht(correo).toLowerCase(Locale.ROOT);
        String usuarioErht = limpiarErht(nombreUsuario).toLowerCase(Locale.ROOT);

        if (n.length() < 5 || !n.contains(" ")) {
            throw new ExcepcionReglaNegocio("Escribe tu nombre completo (nombres y apellidos).");
        }
        if (!d.matches("\\d{8}")) {
            throw new ExcepcionReglaNegocio("El DNI debe tener 8 dígitos.");
        }
        if (!celErht.matches("9\\d{8}")) {
            throw new ExcepcionReglaNegocio("El celular debe tener 9 dígitos y empezar con 9.");
        }
        if (!correoLimpioErht.isEmpty() && !correoLimpioErht.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new ExcepcionReglaNegocio("El correo no es válido.");
        }
        if (!usuarioErht.matches("[a-z0-9._]{4,30}")) {
            throw new ExcepcionReglaNegocio("El usuario debe tener de 4 a 30 caracteres: letras, números, punto o guion bajo.");
        }
        if (clave == null || clave.length() < 6) {
            throw new ExcepcionReglaNegocio("La contraseña debe tener al menos 6 caracteres.");
        }
        Integer existeUsuarioErht = jdbcErht.queryForObject("SELECT COUNT(*) FROM usuario WHERE LOWER(username) = ?",
                Integer.class, usuarioErht);
        if (existeUsuarioErht != null && existeUsuarioErht > 0) {
            throw new ExcepcionReglaNegocio("El usuario «" + usuarioErht + "» ya existe. Elige otro.");
        }
        Integer existeDniErht = jdbcErht.queryForObject("SELECT COUNT(*) FROM cliente WHERE numero_documento = ?",
                Integer.class, d);
        if (existeDniErht != null && existeDniErht > 0) {
            // No se vincula solo: cualquiera que conozca el DNI podría ver los alquileres de esa persona.
            throw new ExcepcionReglaNegocio("Ya hay un cliente registrado con el DNI " + d
                    + ". Pide en la tienda que te creen el acceso a tu cuenta.");
        }

        Integer idClienteErht = jdbcErht.queryForObject("""
                INSERT INTO cliente (tipo_documento, numero_documento, nombres, telefono, email)
                VALUES ('DNI', ?, ?, ?, ?)
                RETURNING id_cliente
                """, Integer.class, d, n, celErht, correoLimpioErht.isEmpty() ? null : correoLimpioErht);
        jdbcErht.update("""
                INSERT INTO usuario (username, password_hash, rol, id_cliente)
                VALUES (?, ?, 'CLIENTE', ?)
                """, usuarioErht, BCRYPT.encode(clave), idClienteErht);
        return autenticar(usuarioErht, clave);
    }

    // ------------------------------------------------------------------ administración (solo ADMIN)

    public java.util.List<pe.ehuanca.trajes.modelo.UsuarioFila> listar() {
        return jdbcErht.query("""
                SELECT u.id_usuario, u.username, u.rol, u.activo, u.fecha_creacion,
                       NULLIF(CONCAT_WS(' ', c.nombres, c.apellidos), '') AS cliente
                FROM usuario u
                LEFT JOIN cliente c ON c.id_cliente = u.id_cliente
                ORDER BY CASE u.rol WHEN 'ADMIN' THEN 0 WHEN 'VENDEDOR' THEN 1 ELSE 2 END, u.username
                """, (rs, i) -> new pe.ehuanca.trajes.modelo.UsuarioFila(rs.getInt("id_usuario"), rs.getString("username"),
                rs.getString("rol"), rs.getString("cliente"), rs.getBoolean("activo"),
                rs.getTimestamp("fecha_creacion").toLocalDateTime()));
    }

    /** Crea un usuario de la tienda (ADMIN o VENDEDOR). Los clientes se registran solos desde el login. */
    public void crearInterno(String nombreUsuario, String clave, String rol) {
        String usuarioErht = limpiarErht(nombreUsuario).toLowerCase(Locale.ROOT);
        if (!usuarioErht.matches("[a-z0-9._]{4,30}")) {
            throw new ExcepcionReglaNegocio("El usuario debe tener de 4 a 30 caracteres: letras, números, punto o guion bajo.");
        }
        if (!Usuario.ADMIN.equals(rol) && !Usuario.VENDEDOR.equals(rol)) {
            throw new ExcepcionReglaNegocio("Elige el rol: ADMIN o VENDEDOR.");
        }
        if (clave == null || clave.length() < 6) {
            throw new ExcepcionReglaNegocio("La contraseña debe tener al menos 6 caracteres.");
        }
        Integer existeErht = jdbcErht.queryForObject("SELECT COUNT(*) FROM usuario WHERE LOWER(username) = ?", Integer.class, usuarioErht);
        if (existeErht != null && existeErht > 0) {
            throw new ExcepcionReglaNegocio("El usuario «" + usuarioErht + "» ya existe.");
        }
        jdbcErht.update("INSERT INTO usuario (username, password_hash, rol) VALUES (?, ?, ?)", usuarioErht, BCRYPT.encode(clave), rol);
    }

    /** Activa o desactiva una cuenta. Nadie puede desactivarse a sí mismo ni dejar la tienda sin ADMIN activo. */
    public void cambiarActivo(int idUsuario, boolean activo, int idUsuarioActual) {
        if (!activo && idUsuario == idUsuarioActual) {
            throw new ExcepcionReglaNegocio("No puedes desactivar tu propia cuenta.");
        }
        if (!activo) {
            Integer otrosAdminErht = jdbcErht.queryForObject("""
                    SELECT COUNT(*) FROM usuario
                    WHERE rol = 'ADMIN' AND activo AND id_usuario <> ?
                      AND EXISTS (SELECT 1 FROM usuario x WHERE x.id_usuario = ? AND x.rol = 'ADMIN')
                    """, Integer.class, idUsuario, idUsuario);
            Integer esAdminErht = jdbcErht.queryForObject("SELECT COUNT(*) FROM usuario WHERE id_usuario = ? AND rol = 'ADMIN'",
                    Integer.class, idUsuario);
            if (esAdminErht != null && esAdminErht > 0 && (otrosAdminErht == null || otrosAdminErht == 0)) {
                throw new ExcepcionReglaNegocio("Debe quedar al menos un ADMIN activo.");
            }
        }
        jdbcErht.update("UPDATE usuario SET activo = ? WHERE id_usuario = ?", activo, idUsuario);
    }

    /** El ADMIN pone una contraseña nueva (por ejemplo, si alguien la olvidó). */
    public void restablecerClave(int idUsuario, String nueva) {
        if (nueva == null || nueva.length() < 6) {
            throw new ExcepcionReglaNegocio("La contraseña debe tener al menos 6 caracteres.");
        }
        jdbcErht.update("UPDATE usuario SET password_hash = ? WHERE id_usuario = ?", BCRYPT.encode(nueva), idUsuario);
    }

    /** Cambia la contraseña verificando la actual. */
    public void cambiarClave(int idUsuario, String actual, String nueva, String repetida) {
        String claveCifradaErht = jdbcErht.queryForObject("SELECT password_hash FROM usuario WHERE id_usuario = ?", String.class,
                idUsuario);
        if (actual == null || !BCRYPT.matches(actual, claveCifradaErht)) {
            throw new ExcepcionReglaNegocio("La contraseña actual no es correcta.");
        }
        if (nueva == null || nueva.length() < 6) {
            throw new ExcepcionReglaNegocio("La nueva contraseña debe tener al menos 6 caracteres.");
        }
        if (!nueva.equals(repetida)) {
            throw new ExcepcionReglaNegocio("La nueva contraseña y su repetición no coinciden.");
        }
        if (nueva.equals(actual)) {
            throw new ExcepcionReglaNegocio("La nueva contraseña debe ser distinta de la actual.");
        }
        jdbcErht.update("UPDATE usuario SET password_hash = ? WHERE id_usuario = ?", BCRYPT.encode(nueva), idUsuario);
    }

    private static String limpiarErht(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }
}
