package com.trajes.modelo;

import java.math.BigDecimal;

public record ClienteFila(Integer idCliente, String tipoDocumento, String numeroDocumento, String nombreCompleto, String telefono, String email, long alquileres, BigDecimal totalGastado) {}
