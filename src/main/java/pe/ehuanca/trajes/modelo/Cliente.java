/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

import java.time.LocalDate;

/**
 * Datos completos de un cliente.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record Cliente(Integer idCliente, String tipoDocumento, String numeroDocumento, String nombres, String apellidos, String telefono, String correo, String direccion, LocalDate fechaRegistro) {}
