package com.trajes.modelo;

import java.math.BigDecimal;

/** Fila del listado de trajes. */
public record TrajeFila(Integer idTraje, String codigo, String danza, String personaje, String genero, String talla, String color, BigDecimal precioAlquiler, BigDecimal garantia, String estado, long vecesAlquilado) {}
