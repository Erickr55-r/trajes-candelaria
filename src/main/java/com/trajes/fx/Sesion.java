package com.trajes.fx;

import com.trajes.modelo.Usuario;

/** Sesión de la aplicación (singleton): quién inició sesión. */
public final class Sesion {

    private static final Sesion INSTANCIA = new Sesion();

    private Usuario usuario;

    private Sesion() {
    }

    public static Sesion get() {
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

    /** Id del cliente de la sesión: todo lo que ve el portal se filtra por este id. */
    public int getCliente() {
        return idCliente();
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
            throw new IllegalStateException("El usuario " + usuario.username() + " no está vinculado a un cliente.");
        }
        return id;
    }
}
