package com.trajes.modelo;

import java.math.BigDecimal;

/** Fila del listado de clientes. */
public record ClienteFila(Integer idCliente, String tipoDocumento, String numeroDocumento, String nombreCompleto, String telefono, String email, long alquileres, BigDecimal totalGastado) {}
