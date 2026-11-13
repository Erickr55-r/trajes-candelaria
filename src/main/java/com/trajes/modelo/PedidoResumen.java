package com.trajes.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resumen de un pedido personalizado (viene de la vista v_pedidos_resumen). */
public record PedidoResumen(Integer idPedido, Integer idCliente, String cliente, Integer idDanza, String danza, String talla, String tipo, String estado, LocalDate fechaPedido, LocalDate fechaEntrega, long numPartes, long numImplementos, BigDecimal montoTotal, BigDecimal adelanto, BigDecimal saldo, String observacion, BigDecimal precioEstimado) {}
