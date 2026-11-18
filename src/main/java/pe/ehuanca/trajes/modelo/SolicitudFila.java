/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Solicitud de alquiler hecha por un cliente desde su portal, pendiente de aprobación.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record SolicitudFila(int idAlquiler, int idCliente, String cliente, String telefono, String conjunto,
                            LocalDate salida, LocalDate devolucion, long numTrajes, BigDecimal montoAlquiler,
                            BigDecimal montoGarantia, String observacion, LocalDateTime creadoEn) {}
