package com.trajes.modelo;

import java.math.BigDecimal;

/** Pieza que se puede marcar como dañada al devolver. */
public record PiezaAlquiler(Integer idPieza, String etiqueta, BigDecimal valorReposicion) {}
