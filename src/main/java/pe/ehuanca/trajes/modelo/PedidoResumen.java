/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resumen de un pedido personalizado (viene de la vista v_pedidos_resumen).
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record PedidoResumen(Integer idPedido, Integer idCliente, String cliente, Integer idDanza, String danza, String talla, String tipo, String estado, LocalDate fechaPedido, LocalDate fechaEntrega, long numPartes, long numImplementos, BigDecimal montoTotal, BigDecimal adelanto, BigDecimal saldo, String observacion, BigDecimal precioEstimado) {}
