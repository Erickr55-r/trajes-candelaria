package com.trajes.modelo;

import java.math.BigDecimal;

/** Parte de un pedido con el color elegido. */
public record PedidoParte(Integer idParte, String parte, BigDecimal precioBase, Integer idColor, String color, String hex) {}
