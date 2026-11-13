package com.trajes.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pago registrado en un alquiler. */
public record PagoFila(Integer idPago, LocalDate fechaPago, String concepto, BigDecimal monto, String metodo) {}
