package com.trajes.modelo;

import java.math.BigDecimal;

/** Traje libre para las fechas buscadas. */
public record TrajeDisponible(Integer idTraje, String codigo, String danza, String personaje, String genero, String talla, String color, BigDecimal precioAlquiler, BigDecimal garantia) {}
