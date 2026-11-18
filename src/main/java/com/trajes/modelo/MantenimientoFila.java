package com.trajes.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MantenimientoFila(Integer idMantenimiento, String tipo, LocalDate fechaInicio, LocalDate fechaFin, BigDecimal costo, String detalle) {}
