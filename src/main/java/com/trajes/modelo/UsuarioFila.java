package com.trajes.modelo;

import java.time.LocalDateTime;

public record UsuarioFila(int idUsuario, String username, String rol, String cliente, boolean activo,
                          LocalDateTime fechaCreacion) {}
