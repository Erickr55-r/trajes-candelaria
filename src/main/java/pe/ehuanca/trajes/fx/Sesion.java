/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx;

import pe.ehuanca.trajes.modelo.Usuario;

/**
 * Sesión de la aplicación (singleton): quién inició sesión.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public final class Sesion {

    private static final Sesion INSTANCIA = new Sesion();

    private Usuario usuario;

    private Sesion() {
    }

    public static Sesion actual() {
        return INSTANCIA;
    }

    public void iniciar(Usuario u) {
        this.usuario = u;
    }

    public void cerrar() {
        this.usuario = null;
    }

    public boolean activa() {
        return usuario != null;
    }

    /** Usuario actual (lanza un error si no hay sesión). */
    public Usuario usuario() {
        if (usuario == null) {
            throw new IllegalStateException("No hay una sesión iniciada.");
        }
        return usuario;
    }

    /** Reemplaza los datos del usuario (por ejemplo, después de editar el perfil). */
    public void actualizar(Usuario u) {
        if (usuario != null && u.idUsuario() == usuario.idUsuario()) {
            this.usuario = u;
        }
    }

    /** Id del cliente de la sesión (solo para usuarios CLIENTE). */
    public int idCliente() {
        Integer id = usuario().idCliente();
        if (id == null) {
            throw new IllegalStateException("El usuario " + usuario.nombreUsuario() + " no está vinculado a un cliente.");
        }
        return id;
    }
}
