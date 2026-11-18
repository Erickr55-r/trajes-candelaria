/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

import java.time.LocalDate;

/**
 * Un traje comprometido en un alquiler reservado o entregado (para calcular su próxima fecha libre).
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record Ocupacion(int idTraje, LocalDate salida, LocalDate devolucionPrevista, String estado) {}
