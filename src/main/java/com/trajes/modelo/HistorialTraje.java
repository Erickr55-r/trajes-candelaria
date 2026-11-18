package com.trajes.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HistorialTraje(Integer idAlquiler, String cliente, LocalDate fechaSalida, LocalDate fechaDevolucionPrevista, String estado, BigDecimal precio) {}
