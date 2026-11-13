package com.trajes.modelo;

import java.util.List;

/** Pedido completo: resumen, partes con color e implementos. */
public record PedidoDetalle(PedidoResumen resumen, List<PedidoParte> partes, List<PedidoImplemento> implementos) {}
