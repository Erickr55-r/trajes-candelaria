package com.trajes.modelo;

import java.math.BigDecimal;

/** Pieza de un traje (máscara, pollera, botas...). */
public record PiezaFila(Integer idPieza, String tipo, String descripcion, String estado, BigDecimal valorReposicion) {}
