/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resumen de un alquiler (viene de la vista v_alquileres_resumen).
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record AlquilerResumen(Integer idAlquiler, Integer idCliente, String estado, LocalDate fechaSalida, LocalDate fechaDevolucionPrevista, LocalDate fechaDevolucionReal, String cliente, String conjunto, long numTrajes, BigDecimal montoAlquiler, BigDecimal montoGarantia, BigDecimal montoDanios, BigDecimal pagadoAlquiler, String observacion) {
    public BigDecimal saldo() {
        BigDecimal s = montoAlquiler.subtract(pagadoAlquiler);
        return s.signum() < 0 ? BigDecimal.ZERO : s;
    }
}
