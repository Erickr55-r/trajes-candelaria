package com.trajes.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resumen de un alquiler (viene de la vista v_alquileres_resumen). */
public record AlquilerResumen(Integer idAlquiler, Integer idCliente, String estado, LocalDate fechaSalida, LocalDate fechaDevolucionPrevista, LocalDate fechaDevolucionReal, String cliente, String conjunto, long numTrajes, BigDecimal montoAlquiler, BigDecimal montoGarantia, BigDecimal montoDanios, BigDecimal pagadoAlquiler, String observacion) {
    /** Lo que falta pagar del alquiler (nunca negativo). */
    public BigDecimal saldo() {
        BigDecimal s = montoAlquiler.subtract(pagadoAlquiler);
        return s.signum() < 0 ? BigDecimal.ZERO : s;
    }
}
