/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

import java.util.List;

/**
 * Pedido completo: resumen, partes con color e implementos.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record PedidoDetalle(PedidoResumen resumen, List<PedidoParte> partes, List<PedidoImplemento> implementos) {}
