package com.trajes.fx;

import com.trajes.modelo.Usuario;

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

    public Usuario usuario() {
        if (usuario == null) {
            throw new IllegalStateException("No hay una sesión iniciada.");
        }
        return usuario;
    }

    public int getCliente() {
        return idCliente();
    }

    public void actualizar(Usuario u) {
        if (usuario != null && u.idUsuario() == usuario.idUsuario()) {
            this.usuario = u;
        }
    }

    public int idCliente() {
        Integer id = usuario().idCliente();
        if (id == null) {
            throw new IllegalStateException("El usuario " + usuario.username() + " no está vinculado a un cliente.");
        }
        return id;
    }
}
