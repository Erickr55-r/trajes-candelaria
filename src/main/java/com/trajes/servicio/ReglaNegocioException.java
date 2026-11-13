package com.trajes.servicio;

/** Error de una regla del negocio que se muestra tal cual al usuario. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
