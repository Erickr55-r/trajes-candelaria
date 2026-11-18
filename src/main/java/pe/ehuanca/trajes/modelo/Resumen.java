/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

import java.math.BigDecimal;

/**
 * Indicadores de la pantalla de inicio.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record Resumen(long disponibles, long fuera, long reservas, long vencidos, long mantenimiento, BigDecimal ingresosMes) {}
