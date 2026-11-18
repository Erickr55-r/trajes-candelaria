package com.trajes.fx.vistas;

import com.trajes.fx.Servicios;
import com.trajes.fx.Sesion;
import com.trajes.fx.Ui;
import com.trajes.modelo.Usuario;
import com.trajes.modelo.UsuarioFila;
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

public class UsuariosVista extends VBox {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Servicios s;
    private final TableView<UsuarioFila> tabla = Ui.tabla("No hay usuarios.");

    public UsuariosVista(Servicios s) {
        if (!Usuario.ADMIN.equals(Sesion.get().usuario().rol())) {
            throw new IllegalStateException("Solo el ADMIN puede administrar usuarios.");
        }
        this.s = s;
        getStyleClass().add("contenido");

        tabla.getColumns().add(Ui.colTexto("Usuario", f -> "@" + f.username(), 160));
        tabla.getColumns().add(Ui.colEstado("Rol", UsuarioFila::rol, 120));
        tabla.getColumns().add(Ui.colTexto("Cliente vinculado", UsuarioFila::cliente, 240));
        tabla.getColumns().add(Ui.colEstado("Estado", f -> f.activo() ? "ACTIVO" : "INACTIVO", 120));
        tabla.getColumns().add(Ui.colTexto("Creado", f -> f.fechaCreacion().format(FECHA_HORA), 150));
        tabla.setMinHeight(360);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button activar = new Button("Activar / desactivar", Ui.icono(Feather.POWER));
        activar.setOnAction(e -> conSeleccion(f -> {
            String accion = f.activo() ? "DESACTIVAR" : "ACTIVAR";
            if (Ui.confirmar("¿" + accion + " la cuenta @" + f.username() + "?"
                    + (f.activo() ? "\nNo podrá iniciar sesión hasta que la actives de nuevo." : ""))) {
                Ui.ejecutar(() -> {
                    s.usuarios().cambiarActivo(f.idUsuario(), !f.activo(), Sesion.get().usuario().idUsuario());
                    cargar();
                });
            }
        }));
        Button clave = new Button("Restablecer contraseña", Ui.icono(Feather.KEY));
        clave.setOnAction(e -> conSeleccion(f -> Ui.pedirTexto("Nueva contraseña para @" + f.username(),
                        "Comunícale la contraseña nueva a la persona; podrá cambiarla desde su perfil.",
                        "NUEVA CONTRASEÑA", "", "Mínimo 6 caracteres")
                .ifPresent(nueva -> {
                    if (Ui.ejecutar(() -> s.usuarios().restablecerClave(f.idUsuario(), nueva))) {
                        Ui.info("Contraseña de @" + f.username() + " restablecida.");
                    }
                })));

        HBox cabecera = new HBox(10, Ui.encabezado("Administración", "Usuarios",
                "Cuentas de la tienda y de los clientes. Los clientes se registran solos desde la pantalla de ingreso."),
                Ui.espacio(), clave, activar);
        cabecera.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(cabecera, formularioNuevo(), tabla);
        cargar();
    }

    private HBox formularioNuevo() {
        TextField usuario = Ui.campo("", "Ej.: rosa.vendedora");
        PasswordField clave = new PasswordField();
        clave.setPromptText("Mínimo 6 caracteres");
        ComboBox<String> rol = Ui.comboTexto(List.of(Usuario.VENDEDOR, Usuario.ADMIN), null);
        rol.setPrefWidth(150);
        HBox.setHgrow(usuario, Priority.ALWAYS);
        HBox.setHgrow(clave, Priority.ALWAYS);
        Button crear = Ui.estilo(new Button("Crear usuario", Ui.icono(Feather.USER_PLUS)), "boton-primario");
        crear.setOnAction(e -> {
            if (Ui.ejecutar(() -> s.usuarios().crearInterno(usuario.getText(), clave.getText(), rol.getValue()))) {
                Ui.info("Usuario @" + usuario.getText().trim().toLowerCase() + " creado.");
                usuario.clear();
                clave.clear();
                cargar();
            }
        });
        Label titulo = Ui.etiqueta("Nuevo usuario de la tienda");
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
            Ui.error("Primero elige un usuario de la lista.");
            return;
        }
        accion.accept(f);
    }
}
