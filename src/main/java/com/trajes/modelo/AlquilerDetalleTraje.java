package com.trajes.modelo;

import java.math.BigDecimal;

public record AlquilerDetalleTraje(Integer idDetalle, Integer idTraje, String codigo, String danza, String personaje, String talla, BigDecimal precio, BigDecimal garantia) {}
