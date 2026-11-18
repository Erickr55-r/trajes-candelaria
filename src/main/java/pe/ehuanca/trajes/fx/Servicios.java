/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx;

import pe.ehuanca.trajes.servicio.AlquilerServicio;
import pe.ehuanca.trajes.servicio.ClienteServicio;
import pe.ehuanca.trajes.servicio.PersonalizadorServicio;
import pe.ehuanca.trajes.servicio.ReporteServicio;
import pe.ehuanca.trajes.servicio.TrajeServicio;
import pe.ehuanca.trajes.servicio.UsuarioServicio;

/**
 * Los servicios de Spring que usan las pantallas.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public record Servicios(AlquilerServicio alquileres, TrajeServicio trajes, ClienteServicio clientes,
                        ReporteServicio reportes, PersonalizadorServicio personalizador,
                        UsuarioServicio usuarios) {}
