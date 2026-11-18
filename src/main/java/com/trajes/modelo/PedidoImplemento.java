package com.trajes.modelo;

import java.math.BigDecimal;

public record PedidoImplemento(Integer idImplemento, String implemento, String parteDestino, BigDecimal precio, int cantidad, BigDecimal subtotal) {}
