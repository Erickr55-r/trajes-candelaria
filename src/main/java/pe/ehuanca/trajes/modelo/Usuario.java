/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

/**
 * Usuario que inició sesión. {@code idCliente} solo viene en los usuarios CLIENTE (y opcionalmente en otros).
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record Usuario(int idUsuario, String nombreUsuario, String rol, Integer idCliente, String nombreCliente) {

    public static final String ADMIN = "ADMIN";
    public static final String VENDEDOR = "VENDEDOR";
    public static final String CLIENTE = "CLIENTE";

    public boolean esCliente() {
        return CLIENTE.equals(rol);
    }

    public String nombreVisible() {
        return nombreCliente != null && !nombreCliente.isBlank() ? nombreCliente : nombreUsuario;
    }

    public String rolVisible() {
        return switch (rol) {
            case ADMIN -> "Administrador";
            case VENDEDOR -> "Vendedor";
            default -> "Cliente";
        };
    }
}
