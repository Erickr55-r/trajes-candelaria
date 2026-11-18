/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

/**
 * Fila del listado de conjuntos de danza.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record ConjuntoFila(Integer idConjunto, String nombre, String danza, String delegado, String telefono, long alquileres) {}
