/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

import java.math.BigDecimal;

/**
 * Implemento de un pedido con su cantidad.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record PedidoImplemento(Integer idImplemento, String implemento, String parteDestino, BigDecimal precio, int cantidad, BigDecimal subtotal) {}
