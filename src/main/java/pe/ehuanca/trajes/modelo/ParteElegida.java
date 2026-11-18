/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

/**
 * Prenda de un diseño: la parte del traje y su color (de la paleta o libre, siempre con su hex).
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record ParteElegida(int idParte, Integer idColor, String colorHex) {}
