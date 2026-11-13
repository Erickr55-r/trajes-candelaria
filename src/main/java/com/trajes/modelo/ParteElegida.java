package com.trajes.modelo;

/** Prenda de un diseño: la parte del traje y su color (de la paleta o libre, siempre con su hex). */
public record ParteElegida(int idParte, Integer idColor, String hex) {}
