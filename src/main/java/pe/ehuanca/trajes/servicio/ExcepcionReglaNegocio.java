/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.servicio;

/**
 * Error de una regla del negocio que se muestra tal cual al usuario.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class ExcepcionReglaNegocio extends RuntimeException {

    public ExcepcionReglaNegocio(String mensaje) {
        super(mensaje);
    }
}
