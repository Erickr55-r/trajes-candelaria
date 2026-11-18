/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.portal;

import pe.ehuanca.trajes.fx.VistaPortalCliente;
import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.Sesion;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.Cliente;
import pe.ehuanca.trajes.modelo.Usuario;
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

/**
 * Perfil del cliente: sus datos de contacto y el cambio de contraseña.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class PerfilVista extends VBox {

    public PerfilVista(Servicios s, VistaPortalCliente portal) {
        getStyleClass().add("contenido");
        int idCliente = Sesion.actual().idCliente();   // solo sus propios datos
        Usuario u = Sesion.actual().usuario();
        Cliente c = s.clientes().buscar(idCliente);

        // ---------------- datos
        TextField documento = UtilidadesInterfaz.campo(c.tipoDocumento() + " " + c.numeroDocumento(), "");
        documento.setEditable(false);
        documento.getStyleClass().add("campo-bloqueado");
        TextField usuario = UtilidadesInterfaz.campo("@" + u.nombreUsuario(), "");
        usuario.setEditable(false);
        usuario.getStyleClass().add("campo-bloqueado");
        TextField nombres = UtilidadesInterfaz.campo(c.nombres(), "Nombres");
        TextField apellidos = UtilidadesInterfaz.campo(c.apellidos() == null ? "" : c.apellidos(), "Apellidos");
        TextField telefono = UtilidadesInterfaz.campo(c.telefono() == null ? "" : c.telefono(), "9 dígitos");
        TextField correo = UtilidadesInterfaz.campo(c.correo() == null ? "" : c.correo(), "tu@correo.com");
        TextField direccion = UtilidadesInterfaz.campo(c.direccion() == null ? "" : c.direccion(), "Opcional");

        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(12);
        fila(g, 0, "Documento", documento, "Usuario", usuario);
        fila(g, 1, "Nombres", nombres, "Apellidos", apellidos);
        fila(g, 2, "Celular", telefono, "Correo", correo);
        g.add(campo("Dirección", direccion), 0, 3, 2, 1);

        Label avisoDatos = new Label();
        avisoDatos.getStyleClass().add("exito-suave");
        avisoDatos.setVisible(false);
        Button guardar = UtilidadesInterfaz.estilo(new Button("Guardar cambios", UtilidadesInterfaz.icono(Feather.SAVE)), "boton-primario");
        guardar.setOnAction(e -> {
            avisoDatos.setVisible(false);
            if (UtilidadesInterfaz.ejecutar(() -> s.clientes().actualizarPerfil(idCliente, nombres.getText(), apellidos.getText(),
                    telefono.getText(), correo.getText(), direccion.getText()))) {
                String nombre = (nombres.getText().trim() + " " + apellidos.getText().trim()).trim();
                Sesion.actual().actualizar(new Usuario(u.idUsuario(), u.nombreUsuario(), u.rol(), u.idCliente(), nombre));
                portal.refrescarCabecera();
                avisoDatos.setText("Tus datos se guardaron.");
                avisoDatos.setVisible(true);
            }
        });
        HBox pieDatos = new HBox(12, avisoDatos, UtilidadesInterfaz.espacio(), guardar);
        pieDatos.setAlignment(Pos.CENTER_LEFT);
        VBox datos = new VBox(16, seccion("Mis datos", "El documento no se puede cambiar aquí; si hay un error, "
                + "avísanos en la tienda."), g, pieDatos);
        datos.getStyleClass().add("tarjeta-blanca");
        HBox.setHgrow(datos, Priority.ALWAYS);

        // ---------------- contraseña
        PasswordField actual = new PasswordField();
        PasswordField nueva = new PasswordField();
        PasswordField repetir = new PasswordField();
        actual.setPromptText("Contraseña actual");
        nueva.setPromptText("Mínimo 6 caracteres");
        repetir.setPromptText("Repite la nueva");
        Label avisoClave = new Label();
        avisoClave.getStyleClass().add("exito-suave");
        avisoClave.setVisible(false);
        Button cambiar = UtilidadesInterfaz.estilo(new Button("Cambiar contraseña", UtilidadesInterfaz.icono(Feather.LOCK)), "boton-primario");
        cambiar.setMaxWidth(Double.MAX_VALUE);
        cambiar.setOnAction(e -> {
            avisoClave.setVisible(false);
            if (UtilidadesInterfaz.ejecutar(() -> s.usuarios().cambiarClave(u.idUsuario(), actual.getText(), nueva.getText(),
                    repetir.getText()))) {
                actual.clear();
                nueva.clear();
                repetir.clear();
                avisoClave.setText("Tu contraseña se cambió.");
                avisoClave.setVisible(true);
            }
        });
        VBox clave = new VBox(14, seccion("Contraseña", "Usa una que no uses en otros sitios."),
                campo("Contraseña actual", actual), campo("Nueva contraseña", nueva),
                campo("Repite la nueva", repetir), cambiar, avisoClave);
        clave.getStyleClass().add("tarjeta-blanca");
        clave.setPrefWidth(360);
        clave.setMinWidth(320);

        HBox columnas = new HBox(22, datos, clave);
        getChildren().addAll(UtilidadesInterfaz.encabezado("Mi cuenta", "Perfil", "Hola, " + u.nombreVisible() + "."), columnas);
    }

    private static VBox seccion(String titulo, String texto) {
        return new VBox(4, UtilidadesInterfaz.seccion(titulo), UtilidadesInterfaz.subtitulo(texto));
    }

    private static VBox campo(String titulo, javafx.scene.control.Control control) {
        Label l = new Label(UtilidadesInterfaz.espaciado(titulo));
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
