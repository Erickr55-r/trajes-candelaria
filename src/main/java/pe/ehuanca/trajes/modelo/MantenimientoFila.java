/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Registro de lavado o reparación de un traje.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record MantenimientoFila(Integer idMantenimiento, String tipo, LocalDate fechaInicio, LocalDate fechaFin, BigDecimal costo, String detalle) {}
