package com.trajes.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Registro de lavado o reparación de un traje. */
public record MantenimientoFila(Integer idMantenimiento, String tipo, LocalDate fechaInicio, LocalDate fechaFin, BigDecimal costo, String detalle) {}
