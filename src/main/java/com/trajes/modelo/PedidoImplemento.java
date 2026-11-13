package com.trajes.modelo;

import java.math.BigDecimal;

/** Implemento de un pedido con su cantidad. */
public record PedidoImplemento(Integer idImplemento, String implemento, String parteDestino, BigDecimal precio, int cantidad, BigDecimal subtotal) {}
