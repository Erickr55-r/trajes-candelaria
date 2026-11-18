package com.trajes.modelo;

import java.math.BigDecimal;

public record PedidoParte(Integer idParte, String parte, BigDecimal precioBase, Integer idColor, String color, String hex) {}
