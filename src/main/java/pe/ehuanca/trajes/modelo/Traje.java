/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos completos de un traje (detalle y formulario).
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record Traje(Integer idTraje, String codigo, Integer idDanza, String danza, String personaje, String genero, String talla, String color, BigDecimal precioAlquiler, BigDecimal garantia, LocalDate fechaAdquisicion, String estado) {}
