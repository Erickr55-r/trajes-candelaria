package com.trajes.modelo;

public record Usuario(int idUsuario, String username, String rol, Integer idCliente, String nombreCliente) {

    public static final String ADMIN = "ADMIN";
    public static final String VENDEDOR = "VENDEDOR";
    public static final String CLIENTE = "CLIENTE";

    public boolean esCliente() {
        return CLIENTE.equals(rol);
    }

    public String nombreVisible() {
        return nombreCliente != null && !nombreCliente.isBlank() ? nombreCliente : username;
    }

    public String rolVisible() {
        return switch (rol) {
            case ADMIN -> "Administrador";
            case VENDEDOR -> "Vendedor";
            default -> "Cliente";
        };
    }
}
