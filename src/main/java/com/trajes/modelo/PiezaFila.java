package com.trajes.modelo;

import java.math.BigDecimal;

public record PiezaFila(Integer idPieza, String tipo, String descripcion, String estado, BigDecimal valorReposicion) {}
