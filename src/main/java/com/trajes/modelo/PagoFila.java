package com.trajes.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoFila(Integer idPago, LocalDate fechaPago, String concepto, BigDecimal monto, String metodo) {}
