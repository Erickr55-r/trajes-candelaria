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

    private final Servicios sErht;
    private final TableView<UsuarioFila> tablaErht = UtilidadesInterfaz.tabla("No hay usuarios.");

    public UsuariosVista(Servicios s) {
        if (!Usuario.ADMIN.equals(Sesion.actual().usuario().rol())) {
            throw new IllegalStateException("Solo el ADMIN puede administrar usuarios.");
        }
        this.sErht = s;
        getStyleClass().add("contenido");

        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Usuario", f -> "@" + f.nombreUsuario(), 160));
        tablaErht.getColumns().add(UtilidadesInterfaz.colEstado("Rol", UsuarioFila::rol, 120));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Cliente vinculado", UsuarioFila::cliente, 240));
        tablaErht.getColumns().add(UtilidadesInterfaz.colEstado("Estado", f -> f.activo() ? "ACTIVO" : "INACTIVO", 120));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Creado", f -> f.fechaCreacion().format(FECHA_HORA), 150));
        tablaErht.setMinHeight(360);
        VBox.setVgrow(tablaErht, Priority.ALWAYS);

        Button activarErht = new Button("Activar / desactivar", UtilidadesInterfaz.icono(Feather.POWER));
        activarErht.setOnAction(e -> conSeleccionErht(f -> {
            String accionErht = f.activo() ? "DESACTIVAR" : "ACTIVAR";
            if (UtilidadesInterfaz.confirmar("¿" + accionErht + " la cuenta @" + f.nombreUsuario() + "?"
                    + (f.activo() ? "\nNo podrá iniciar sesión hasta que la actives de nuevo." : ""))) {
                UtilidadesInterfaz.ejecutar(() -> {
                    s.usuarios().cambiarActivo(f.idUsuario(), !f.activo(), Sesion.actual().usuario().idUsuario());
                    cargarErht();
                });
            }
        }));
        Button claveErht = new Button("Restablecer contraseña", UtilidadesInterfaz.icono(Feather.KEY));
        claveErht.setOnAction(e -> conSeleccionErht(f -> UtilidadesInterfaz.pedirTexto("Nueva contraseña para @" + f.nombreUsuario(),
                        "Comunícale la contraseña nueva a la persona; podrá cambiarla desde su perfil.",
                        "NUEVA CONTRASEÑA", "", "Mínimo 6 caracteres")
                .ifPresent(nueva -> {
                    if (UtilidadesInterfaz.ejecutar(() -> s.usuarios().restablecerClave(f.idUsuario(), nueva))) {
                        UtilidadesInterfaz.mostrarInformacion("Contraseña de @" + f.nombreUsuario() + " restablecida.");
                    }
                })));

        HBox cabeceraErht = new HBox(10, UtilidadesInterfaz.encabezado("Administración", "Usuarios",
                "Cuentas de la tienda y de los clientes. Los clientes se registran solos desde la pantalla de ingreso."),
                UtilidadesInterfaz.espacio(), claveErht, activarErht);
        cabeceraErht.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(cabeceraErht, formularioNuevoErht(), tablaErht);
        cargarErht();
    }

    private HBox formularioNuevoErht() {
        TextField usuarioErht = UtilidadesInterfaz.campo("", "Ej.: rosa.vendedora");
        PasswordField claveErht = new PasswordField();
        claveErht.setPromptText("Mínimo 6 caracteres");
        ComboBox<String> rolErht = UtilidadesInterfaz.comboTexto(List.of(Usuario.VENDEDOR, Usuario.ADMIN), null);
        rolErht.setPrefWidth(150);
        HBox.setHgrow(usuarioErht, Priority.ALWAYS);
        HBox.setHgrow(claveErht, Priority.ALWAYS);
        Button crearErht = UtilidadesInterfaz.estilo(new Button("Crear usuario", UtilidadesInterfaz.icono(Feather.USER_PLUS)), "boton-primario");
        crearErht.setOnAction(e -> {
            if (UtilidadesInterfaz.ejecutar(() -> sErht.usuarios().crearInterno(usuarioErht.getText(), claveErht.getText(), rolErht.getValue()))) {
                UtilidadesInterfaz.mostrarInformacion("Usuario @" + usuarioErht.getText().trim().toLowerCase() + " creado.");
                usuarioErht.clear();
                claveErht.clear();
                cargarErht();
            }
        });
        Label tituloErht = UtilidadesInterfaz.etiqueta("Nuevo usuario de la tienda");
        HBox cajaErht = new HBox(12, tituloErht, usuarioErht, claveErht, rolErht, crearErht);
        cajaErht.setAlignment(Pos.CENTER_LEFT);
        cajaErht.getStyleClass().add("barra-filtros");
        return cajaErht;
    }

    private void cargarErht() {
        tablaErht.getItems().setAll(sErht.usuarios().listar());
    }

    private void conSeleccionErht(Consumer<UsuarioFila> accion) {
        UsuarioFila f = tablaErht.getSelectionModel().getSelectedItem();
        if (f == null) {
            UtilidadesInterfaz.mostrarError("Primero elige un usuario de la lista.");
            return;
        }
        accion.accept(f);
    }
}
