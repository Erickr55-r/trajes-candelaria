package com.trajes.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos completos de un traje (detalle y formulario). */
public record Traje(Integer idTraje, String codigo, Integer idDanza, String danza, String personaje, String genero, String talla, String color, BigDecimal precioAlquiler, BigDecimal garantia, LocalDate fechaAdquisicion, String estado) {}
