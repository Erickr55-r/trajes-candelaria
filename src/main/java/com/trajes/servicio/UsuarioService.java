package com.trajes.servicio;

import com.trajes.modelo.Usuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.trajes.servicio.TrajeService.tieneTexto;

/** Inicio de sesión (contraseñas BCrypt) y registro de cuentas de cliente. */
@Service
public class UsuarioService {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();
    /** Hash de relleno: si el usuario no existe se compara igual, para no delatarlo por el tiempo de respuesta. */
    private static final String HASH_RELLENO = BCRYPT.encode("relleno-sin-uso");
    private static final String CREDENCIALES_MAL = "Usuario o contraseña incorrectos.";

    private final JdbcTemplate jdbc;

    public UsuarioService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Devuelve el usuario si el nombre y la contraseña son correctos y la cuenta está activa. */
    public Usuario autenticar(String username, String password) {
        if (!tieneTexto(username) || password == null || password.isEmpty()) {
            throw new ReglaNegocioException("Escribe tu usuario y tu contraseña.");
        }
        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT u.id_usuario, u.username, u.password_hash, u.rol, u.id_cliente, u.activo,
                       CONCAT_WS(' ', c.nombres, c.apellidos) AS nombre
                FROM usuario u
                LEFT JOIN cliente c ON c.id_cliente = u.id_cliente
                WHERE LOWER(u.username) = LOWER(?)
                """, username.trim());
        if (filas.isEmpty()) {
            BCRYPT.matches(password, HASH_RELLENO);
            throw new ReglaNegocioException(CREDENCIALES_MAL);
        }
        Map<String, Object> f = filas.get(0);
        if (!BCRYPT.matches(password, (String) f.get("password_hash"))) {
            throw new ReglaNegocioException(CREDENCIALES_MAL);
        }
        if (!Boolean.TRUE.equals(f.get("activo"))) {
            throw new ReglaNegocioException("Tu cuenta está desactivada. Comunícate con la tienda.");
        }
        String nombre = (String) f.get("nombre");
        return new Usuario((Integer) f.get("id_usuario"), (String) f.get("username"), (String) f.get("rol"),
                (Integer) f.get("id_cliente"), nombre == null || nombre.isBlank() ? null : nombre);
    }

    /**
     * Crea el cliente y su usuario CLIENTE en una sola transacción (si algo falla, no queda nada a medias)
     * y devuelve el usuario ya autenticado.
     */
    @Transactional
    public Usuario registrarCliente(String nombre, String dni, String celular, String correo,
                                    String username, String password) {
        String n = limpiar(nombre);
        String d = limpiar(dni);
        String cel = limpiar(celular).replace(" ", "");
        String mail = limpiar(correo).toLowerCase(Locale.ROOT);
        String user = limpiar(username).toLowerCase(Locale.ROOT);

        if (n.length() < 5 || !n.contains(" ")) {
            throw new ReglaNegocioException("Escribe tu nombre completo (nombres y apellidos).");
        }
        if (!d.matches("\\d{8}")) {
            throw new ReglaNegocioException("El DNI debe tener 8 dígitos.");
        }
        if (!cel.matches("9\\d{8}")) {
            throw new ReglaNegocioException("El celular debe tener 9 dígitos y empezar con 9.");
        }
        if (!mail.isEmpty() && !mail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new ReglaNegocioException("El correo no es válido.");
        }
        if (!user.matches("[a-z0-9._]{4,30}")) {
            throw new ReglaNegocioException("El usuario debe tener de 4 a 30 caracteres: letras, números, punto o guion bajo.");
        }
        if (password == null || password.length() < 6) {
            throw new ReglaNegocioException("La contraseña debe tener al menos 6 caracteres.");
        }
        Integer existeUsuario = jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE LOWER(username) = ?",
                Integer.class, user);
        if (existeUsuario != null && existeUsuario > 0) {
            throw new ReglaNegocioException("El usuario «" + user + "» ya existe. Elige otro.");
        }
        Integer existeDni = jdbc.queryForObject("SELECT COUNT(*) FROM cliente WHERE numero_documento = ?",
                Integer.class, d);
        if (existeDni != null && existeDni > 0) {
            // No se vincula solo: cualquiera que conozca el DNI podría ver los alquileres de esa persona.
            throw new ReglaNegocioException("Ya hay un cliente registrado con el DNI " + d
                    + ". Pide en la tienda que te creen el acceso a tu cuenta.");
        }

        Integer idCliente = jdbc.queryForObject("""
                INSERT INTO cliente (tipo_documento, numero_documento, nombres, telefono, email)
                VALUES ('DNI', ?, ?, ?, ?)
                RETURNING id_cliente
                """, Integer.class, d, n, cel, mail.isEmpty() ? null : mail);
        jdbc.update("""
                INSERT INTO usuario (username, password_hash, rol, id_cliente)
                VALUES (?, ?, 'CLIENTE', ?)
                """, user, BCRYPT.encode(password), idCliente);
        return autenticar(user, password);
    }

    // ------------------------------------------------------------------ administración (solo ADMIN)

    public java.util.List<com.trajes.modelo.UsuarioFila> listar() {
        return jdbc.query("""
                SELECT u.id_usuario, u.username, u.rol, u.activo, u.fecha_creacion,
                       NULLIF(CONCAT_WS(' ', c.nombres, c.apellidos), '') AS cliente
                FROM usuario u
                LEFT JOIN cliente c ON c.id_cliente = u.id_cliente
                ORDER BY CASE u.rol WHEN 'ADMIN' THEN 0 WHEN 'VENDEDOR' THEN 1 ELSE 2 END, u.username
                """, (rs, i) -> new com.trajes.modelo.UsuarioFila(rs.getInt("id_usuario"), rs.getString("username"),
                rs.getString("rol"), rs.getString("cliente"), rs.getBoolean("activo"),
                rs.getTimestamp("fecha_creacion").toLocalDateTime()));
    }

    /** Crea un usuario de la tienda (ADMIN o VENDEDOR). Los clientes se registran solos desde el login. */
    public void crearInterno(String username, String password, String rol) {
        String user = limpiar(username).toLowerCase(Locale.ROOT);
        if (!user.matches("[a-z0-9._]{4,30}")) {
            throw new ReglaNegocioException("El usuario debe tener de 4 a 30 caracteres: letras, números, punto o guion bajo.");
        }
        if (!Usuario.ADMIN.equals(rol) && !Usuario.VENDEDOR.equals(rol)) {
            throw new ReglaNegocioException("Elige el rol: ADMIN o VENDEDOR.");
        }
        if (password == null || password.length() < 6) {
            throw new ReglaNegocioException("La contraseña debe tener al menos 6 caracteres.");
        }
        Integer existe = jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE LOWER(username) = ?", Integer.class, user);
        if (existe != null && existe > 0) {
            throw new ReglaNegocioException("El usuario «" + user + "» ya existe.");
        }
        jdbc.update("INSERT INTO usuario (username, password_hash, rol) VALUES (?, ?, ?)", user, BCRYPT.encode(password), rol);
    }

    /** Activa o desactiva una cuenta. Nadie puede desactivarse a sí mismo ni dejar la tienda sin ADMIN activo. */
    public void cambiarActivo(int idUsuario, boolean activo, int idUsuarioActual) {
        if (!activo && idUsuario == idUsuarioActual) {
            throw new ReglaNegocioException("No puedes desactivar tu propia cuenta.");
        }
        if (!activo) {
            Integer otrosAdmin = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM usuario
                    WHERE rol = 'ADMIN' AND activo AND id_usuario <> ?
                      AND EXISTS (SELECT 1 FROM usuario x WHERE x.id_usuario = ? AND x.rol = 'ADMIN')
                    """, Integer.class, idUsuario, idUsuario);
            Integer esAdmin = jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE id_usuario = ? AND rol = 'ADMIN'",
                    Integer.class, idUsuario);
            if (esAdmin != null && esAdmin > 0 && (otrosAdmin == null || otrosAdmin == 0)) {
                throw new ReglaNegocioException("Debe quedar al menos un ADMIN activo.");
            }
        }
        jdbc.update("UPDATE usuario SET activo = ? WHERE id_usuario = ?", activo, idUsuario);
    }

    /** El ADMIN pone una contraseña nueva (por ejemplo, si alguien la olvidó). */
    public void restablecerClave(int idUsuario, String nueva) {
        if (nueva == null || nueva.length() < 6) {
            throw new ReglaNegocioException("La contraseña debe tener al menos 6 caracteres.");
        }
        jdbc.update("UPDATE usuario SET password_hash = ? WHERE id_usuario = ?", BCRYPT.encode(nueva), idUsuario);
    }

    /** Cambia la contraseña verificando la actual. */
    public void cambiarClave(int idUsuario, String actual, String nueva, String repetida) {
        String hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE id_usuario = ?", String.class,
                idUsuario);
        if (actual == null || !BCRYPT.matches(actual, hash)) {
            throw new ReglaNegocioException("La contraseña actual no es correcta.");
        }
        if (nueva == null || nueva.length() < 6) {
            throw new ReglaNegocioException("La nueva contraseña debe tener al menos 6 caracteres.");
        }
        if (!nueva.equals(repetida)) {
            throw new ReglaNegocioException("La nueva contraseña y su repetición no coinciden.");
        }
        if (nueva.equals(actual)) {
            throw new ReglaNegocioException("La nueva contraseña debe ser distinta de la actual.");
        }
        jdbc.update("UPDATE usuario SET password_hash = ? WHERE id_usuario = ?", BCRYPT.encode(nueva), idUsuario);
    }

    private static String limpiar(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }
}
