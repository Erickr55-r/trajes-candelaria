package com.trajes.fx.portal;

import com.trajes.fx.PortalClienteView;
import com.trajes.fx.Servicios;
import com.trajes.fx.Sesion;
import com.trajes.fx.Ui;
import com.trajes.modelo.Cliente;
import com.trajes.modelo.Usuario;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

public class PerfilVista extends VBox {

    public PerfilVista(Servicios s, PortalClienteView portal) {
        getStyleClass().add("contenido");
        int idCliente = Sesion.get().getCliente();   
        Usuario u = Sesion.get().usuario();
        Cliente c = s.clientes().buscar(idCliente);

        TextField documento = Ui.campo(c.tipoDocumento() + " " + c.numeroDocumento(), "");
        documento.setEditable(false);
        documento.getStyleClass().add("campo-bloqueado");
        TextField usuario = Ui.campo("@" + u.username(), "");
        usuario.setEditable(false);
        usuario.getStyleClass().add("campo-bloqueado");
        TextField nombres = Ui.campo(c.nombres(), "Nombres");
        TextField apellidos = Ui.campo(c.apellidos() == null ? "" : c.apellidos(), "Apellidos");
        TextField telefono = Ui.campo(c.telefono() == null ? "" : c.telefono(), "9 dígitos");
        TextField email = Ui.campo(c.email() == null ? "" : c.email(), "tu@correo.com");
        TextField direccion = Ui.campo(c.direccion() == null ? "" : c.direccion(), "Opcional");

        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(12);
        fila(g, 0, "Documento", documento, "Usuario", usuario);
        fila(g, 1, "Nombres", nombres, "Apellidos", apellidos);
        fila(g, 2, "Celular", telefono, "Correo", email);
        g.add(campo("Dirección", direccion), 0, 3, 2, 1);

        Label okDatos = new Label();
        okDatos.getStyleClass().add("ok-suave");
        okDatos.setVisible(false);
        Button guardar = Ui.estilo(new Button("Guardar cambios", Ui.icono(Feather.SAVE)), "boton-primario");
        guardar.setOnAction(e -> {
            okDatos.setVisible(false);
            if (Ui.ejecutar(() -> s.clientes().actualizarPerfil(idCliente, nombres.getText(), apellidos.getText(),
                    telefono.getText(), email.getText(), direccion.getText()))) {
                String nombre = (nombres.getText().trim() + " " + apellidos.getText().trim()).trim();
                Sesion.get().actualizar(new Usuario(u.idUsuario(), u.username(), u.rol(), u.idCliente(), nombre));
                portal.refrescarCabecera();
                okDatos.setText("Tus datos se guardaron.");
                okDatos.setVisible(true);
            }
        });
        HBox pieDatos = new HBox(12, okDatos, Ui.espacio(), guardar);
        pieDatos.setAlignment(Pos.CENTER_LEFT);
        VBox datos = new VBox(16, seccion("Mis datos", "El documento no se puede cambiar aquí; si hay un error, "
                + "avísanos en la tienda."), g, pieDatos);
        datos.getStyleClass().add("tarjeta-blanca");
        HBox.setHgrow(datos, Priority.ALWAYS);

        PasswordField actual = new PasswordField();
        PasswordField nueva = new PasswordField();
        PasswordField repetir = new PasswordField();
        actual.setPromptText("Contraseña actual");
        nueva.setPromptText("Mínimo 6 caracteres");
        repetir.setPromptText("Repite la nueva");
        Label okClave = new Label();
        okClave.getStyleClass().add("ok-suave");
        okClave.setVisible(false);
        Button cambiar = Ui.estilo(new Button("Cambiar contraseña", Ui.icono(Feather.LOCK)), "boton-primario");
        cambiar.setMaxWidth(Double.MAX_VALUE);
        cambiar.setOnAction(e -> {
            okClave.setVisible(false);
            if (Ui.ejecutar(() -> s.usuarios().cambiarClave(u.idUsuario(), actual.getText(), nueva.getText(),
                    repetir.getText()))) {
                actual.clear();
                nueva.clear();
                repetir.clear();
                okClave.setText("Tu contraseña se cambió.");
                okClave.setVisible(true);
            }
        });
        VBox clave = new VBox(14, seccion("Contraseña", "Usa una que no uses en otros sitios."),
                campo("Contraseña actual", actual), campo("Nueva contraseña", nueva),
                campo("Repite la nueva", repetir), cambiar, okClave);
        clave.getStyleClass().add("tarjeta-blanca");
        clave.setPrefWidth(360);
        clave.setMinWidth(320);

        HBox columnas = new HBox(22, datos, clave);
        getChildren().addAll(Ui.encabezado("Mi cuenta", "Perfil", "Hola, " + u.nombreVisible() + "."), columnas);
    }

    private static VBox seccion(String titulo, String texto) {
        return new VBox(4, Ui.seccion(titulo), Ui.subtitulo(texto));
    }

    private static VBox campo(String titulo, javafx.scene.control.Control control) {
        Label l = new Label(Ui.espaciado(titulo));
        l.getStyleClass().add("campo-titulo");
        control.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, l, control);
    }

    private static void fila(GridPane g, int f, String t1, TextField c1, String t2, TextField c2) {
        VBox a = campo(t1, c1);
        VBox b = campo(t2, c2);
        GridPane.setHgrow(a, Priority.ALWAYS);
        GridPane.setHgrow(b, Priority.ALWAYS);
        g.add(a, 0, f);
        g.add(b, 1, f);
    }
}
