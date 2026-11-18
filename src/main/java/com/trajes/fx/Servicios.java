package com.trajes.fx;

import com.trajes.servicio.AlquilerService;
import com.trajes.servicio.ClienteService;
import com.trajes.servicio.PersonalizadorService;
import com.trajes.servicio.ReporteService;
import com.trajes.servicio.TrajeService;
import com.trajes.servicio.UsuarioService;

public record Servicios(AlquilerService alquileres, TrajeService trajes, ClienteService clientes,
                        ReporteService reportes, PersonalizadorService personalizador,
                        UsuarioService usuarios) {}
