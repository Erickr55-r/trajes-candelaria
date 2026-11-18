package com.trajes.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AlquilerResumen(Integer idAlquiler, Integer idCliente, String estado, LocalDate fechaSalida, LocalDate fechaDevolucionPrevista, LocalDate fechaDevolucionReal, String cliente, String conjunto, long numTrajes, BigDecimal montoAlquiler, BigDecimal montoGarantia, BigDecimal montoDanios, BigDecimal pagadoAlquiler, String observacion) {

    public BigDecimal saldo() {
        BigDecimal s = montoAlquiler.subtract(pagadoAlquiler);
        return s.signum() < 0 ? BigDecimal.ZERO : s;
    }
}
