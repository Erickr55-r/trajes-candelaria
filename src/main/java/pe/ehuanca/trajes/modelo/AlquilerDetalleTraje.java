/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

import java.math.BigDecimal;

/**
 * Traje incluido en un alquiler.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record AlquilerDetalleTraje(Integer idDetalle, Integer idTraje, String codigo, String danza, String personaje, String talla, BigDecimal precio, BigDecimal garantia) {}
