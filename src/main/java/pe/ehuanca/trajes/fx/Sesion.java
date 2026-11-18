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

    private Usuario usuarioErht;

    private Sesion() {
    }

    public static Sesion actual() {
        return INSTANCIA;
    }

    public void iniciar(Usuario u) {
        this.usuarioErht = u;
    }

    public void cerrar() {
        this.usuarioErht = null;
    }

    public boolean activa() {
        return usuarioErht != null;
    }

    public Usuario usuario() {
        if (usuarioErht == null) {
            throw new IllegalStateException("No hay una sesión iniciada.");
        }
        return usuarioErht;
    }

    public void actualizar(Usuario u) {
        if (usuarioErht != null && u.idUsuario() == usuarioErht.idUsuario()) {
            this.usuarioErht = u;
        }
    }

    public int idCliente() {
        Integer id = usuario().idCliente();
        if (id == null) {
            throw new IllegalStateException("El usuario " + usuarioErht.nombreUsuario() + " no está vinculado a un cliente.");
        }
        return id;
    }
}
