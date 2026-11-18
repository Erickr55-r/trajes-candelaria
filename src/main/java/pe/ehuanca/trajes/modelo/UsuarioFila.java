/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

import java.time.LocalDateTime;

/**
 * Fila de la pantalla Usuarios (solo ADMIN).
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record UsuarioFila(int idUsuario, String nombreUsuario, String rol, String cliente, boolean activo,
                          LocalDateTime fechaCreacion) {}
