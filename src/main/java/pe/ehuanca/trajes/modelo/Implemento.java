/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

import java.math.BigDecimal;

/**
 * Adorno o accesorio que se agrega al traje (cascabeles, matraca, plumas...).
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record Implemento(Integer idImplemento, String nombre, BigDecimal precio, String parteDestino) {}
