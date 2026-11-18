/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

import java.math.BigDecimal;

/**
 * Traje libre para las fechas buscadas.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record TrajeDisponible(Integer idTraje, String codigo, String danza, String personaje, String genero, String talla, String color, BigDecimal precioAlquiler, BigDecimal garantia) {}
