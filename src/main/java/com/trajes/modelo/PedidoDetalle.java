package com.trajes.modelo;

import java.util.List;

public record PedidoDetalle(PedidoResumen resumen, List<PedidoParte> partes, List<PedidoImplemento> implementos) {}
