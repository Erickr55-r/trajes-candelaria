package com.trajes.modelo;

import java.time.LocalDateTime;

/** Fila de la pantalla Usuarios (solo ADMIN). */
public record UsuarioFila(int idUsuario, String username, String rol, String cliente, boolean activo,
                          LocalDateTime fechaCreacion) {}
