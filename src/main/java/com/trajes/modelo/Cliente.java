package com.trajes.modelo;

import java.time.LocalDate;

public record Cliente(Integer idCliente, String tipoDocumento, String numeroDocumento, String nombres, String apellidos, String telefono, String email, String direccion, LocalDate fechaRegistro) {}
