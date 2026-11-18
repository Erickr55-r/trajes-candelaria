/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

import java.util.List;

/**
 * Resultado genérico de una consulta para la pantalla de reportes.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record Tabla(String titulo, String descripcion, List<String> columnas, List<List<Object>> filas) {}
