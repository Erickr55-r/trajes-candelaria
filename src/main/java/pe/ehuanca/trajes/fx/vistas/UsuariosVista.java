/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.Sesion;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.Usuario;
import pe.ehuanca.trajes.modelo.UsuarioFila;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Usuarios del sistema (solo ADMIN): crear cuentas de la tienda, activar/desactivar y restablecer contraseñas.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class UsuariosVista extends VBox {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Servicios s;
    private final TableView<UsuarioFila> tabla = UtilidadesInterfaz.tabla("No hay usuarios.");

    public UsuariosVista(Servicios s) {
        if (!Usuario.ADMIN.equals(Sesion.actual().usuario().rol())) {
            throw new IllegalStateException("Solo el ADMIN puede administrar usuarios.");
        }
        this.s = s;
        getStyleClass().add("contenido");

        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Usuario", f -> "@" + f.nombreUsuario(), 160));
        tabla.getColumns().add(UtilidadesInterfaz.colEstado("Rol", UsuarioFila::rol, 120));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Cliente vinculado", UsuarioFila::cliente, 240));
        tabla.getColumns().add(UtilidadesInterfaz.colEstado("Estado", f -> f.activo() ? "ACTIVO" : "INACTIVO", 120));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Creado", f -> f.fechaCreacion().format(FECHA_HORA), 150));
        tabla.setMinHeight(360);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button activar = new Button("Activar / desactivar", UtilidadesInterfaz.icono(Feather.POWER));
        activar.setOnAction(e -> conSeleccion(f -> {
            String accion = f.activo() ? "DESACTIVAR" : "ACTIVAR";
            if (UtilidadesInterfaz.confirmar("¿" + accion + " la cuenta @" + f.nombreUsuario() + "?"
                    + (f.activo() ? "\nNo podrá iniciar sesión hasta que la actives de nuevo." : ""))) {
                UtilidadesInterfaz.ejecutar(() -> {
                    s.usuarios().cambiarActivo(f.idUsuario(), !f.activo(), Sesion.actual().usuario().idUsuario());
                    cargar();
                });
            }
        }));
        Button clave = new Button("Restablecer contraseña", UtilidadesInterfaz.icono(Feather.KEY));
        clave.setOnAction(e -> conSeleccion(f -> UtilidadesInterfaz.pedirTexto("Nueva contraseña para @" + f.nombreUsuario(),
                        "Comunícale la contraseña nueva a la persona; podrá cambiarla desde su perfil.",
                        "NUEVA CONTRASEÑA", "", "Mínimo 6 caracteres")
                .ifPresent(nueva -> {
                    if (UtilidadesInterfaz.ejecutar(() -> s.usuarios().restablecerClave(f.idUsuario(), nueva))) {
                        UtilidadesInterfaz.mostrarInformacion("Contraseña de @" + f.nombreUsuario() + " restablecida.");
                    }
                })));

        HBox cabecera = new HBox(10, UtilidadesInterfaz.encabezado("Administración", "Usuarios",
                "Cuentas de la tienda y de los clientes. Los clientes se registran solos desde la pantalla de ingreso."),
                UtilidadesInterfaz.espacio(), clave, activar);
        cabecera.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(cabecera, formularioNuevo(), tabla);
        cargar();
    }

    private HBox formularioNuevo() {
        TextField usuario = UtilidadesInterfaz.campo("", "Ej.: rosa.vendedora");
        PasswordField clave = new PasswordField();
        clave.setPromptText("Mínimo 6 caracteres");
        ComboBox<String> rol = UtilidadesInterfaz.comboTexto(List.of(Usuario.VENDEDOR, Usuario.ADMIN), null);
        rol.setPrefWidth(150);
        HBox.setHgrow(usuario, Priority.ALWAYS);
        HBox.setHgrow(clave, Priority.ALWAYS);
        Button crear = UtilidadesInterfaz.estilo(new Button("Crear usuario", UtilidadesInterfaz.icono(Feather.USER_PLUS)), "boton-primario");
        crear.setOnAction(e -> {
            if (UtilidadesInterfaz.ejecutar(() -> s.usuarios().crearInterno(usuario.getText(), clave.getText(), rol.getValue()))) {
                UtilidadesInterfaz.mostrarInformacion("Usuario @" + usuario.getText().trim().toLowerCase() + " creado.");
                usuario.clear();
                clave.clear();
                cargar();
            }
        });
        Label titulo = UtilidadesInterfaz.etiqueta("Nuevo usuario de la tienda");
        HBox caja = new HBox(12, titulo, usuario, clave, rol, crear);
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.getStyleClass().add("barra-filtros");
        return caja;
    }

    private void cargar() {
        tabla.getItems().setAll(s.usuarios().listar());
    }

    private void conSeleccion(Consumer<UsuarioFila> accion) {
        UsuarioFila f = tabla.getSelectionModel().getSelectedItem();
        if (f == null) {
            UtilidadesInterfaz.mostrarError("Primero elige un usuario de la lista.");
            return;
        }
        accion.accept(f);
    }
}
