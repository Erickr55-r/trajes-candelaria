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
        int idClienteErht = Sesion.actual().idCliente();   // solo sus propios datos
        Usuario u = Sesion.actual().usuario();
        Cliente c = s.clientes().buscar(idClienteErht);

        // ---------------- datos
        TextField documentoErht = UtilidadesInterfaz.campo(c.tipoDocumento() + " " + c.numeroDocumento(), "");
        documentoErht.setEditable(false);
        documentoErht.getStyleClass().add("campo-bloqueado");
        TextField usuarioErht = UtilidadesInterfaz.campo("@" + u.nombreUsuario(), "");
        usuarioErht.setEditable(false);
        usuarioErht.getStyleClass().add("campo-bloqueado");
        TextField nombresErht = UtilidadesInterfaz.campo(c.nombres(), "Nombres");
        TextField apellidosErht = UtilidadesInterfaz.campo(c.apellidos() == null ? "" : c.apellidos(), "Apellidos");
        TextField telefonoErht = UtilidadesInterfaz.campo(c.telefono() == null ? "" : c.telefono(), "9 dígitos");
        TextField correoErht = UtilidadesInterfaz.campo(c.correo() == null ? "" : c.correo(), "tu@correo.com");
        TextField direccionErht = UtilidadesInterfaz.campo(c.direccion() == null ? "" : c.direccion(), "Opcional");

        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(12);
        filaErht(g, 0, "Documento", documentoErht, "Usuario", usuarioErht);
        filaErht(g, 1, "Nombres", nombresErht, "Apellidos", apellidosErht);
        filaErht(g, 2, "Celular", telefonoErht, "Correo", correoErht);
        g.add(campoErht("Dirección", direccionErht), 0, 3, 2, 1);

        Label avisoDatosErht = new Label();
        avisoDatosErht.getStyleClass().add("exito-suave");
        avisoDatosErht.setVisible(false);
        Button guardarErht = UtilidadesInterfaz.estilo(new Button("Guardar cambios", UtilidadesInterfaz.icono(Feather.SAVE)), "boton-primario");
        guardarErht.setOnAction(e -> {
            avisoDatosErht.setVisible(false);
            if (UtilidadesInterfaz.ejecutar(() -> s.clientes().actualizarPerfil(idClienteErht, nombresErht.getText(), apellidosErht.getText(),
                    telefonoErht.getText(), correoErht.getText(), direccionErht.getText()))) {
                String nombreErht = (nombresErht.getText().trim() + " " + apellidosErht.getText().trim()).trim();
                Sesion.actual().actualizar(new Usuario(u.idUsuario(), u.nombreUsuario(), u.rol(), u.idCliente(), nombreErht));
                portal.refrescarCabecera();
                avisoDatosErht.setText("Tus datos se guardaron.");
                avisoDatosErht.setVisible(true);
            }
        });
        HBox pieDatosErht = new HBox(12, avisoDatosErht, UtilidadesInterfaz.espacio(), guardarErht);
        pieDatosErht.setAlignment(Pos.CENTER_LEFT);
        VBox datosErht = new VBox(16, seccionErht("Mis datos", "El documento no se puede cambiar aquí; si hay un error, "
                + "avísanos en la tienda."), g, pieDatosErht);
        datosErht.getStyleClass().add("tarjeta-blanca");
        HBox.setHgrow(datosErht, Priority.ALWAYS);

        // ---------------- contraseña
        PasswordField actualErht = new PasswordField();
        PasswordField nuevaErht = new PasswordField();
        PasswordField repetirErht = new PasswordField();
        actualErht.setPromptText("Contraseña actual");
        nuevaErht.setPromptText("Mínimo 6 caracteres");
        repetirErht.setPromptText("Repite la nueva");
        Label avisoClaveErht = new Label();
        avisoClaveErht.getStyleClass().add("exito-suave");
        avisoClaveErht.setVisible(false);
        Button cambiarErht = UtilidadesInterfaz.estilo(new Button("Cambiar contraseña", UtilidadesInterfaz.icono(Feather.LOCK)), "boton-primario");
        cambiarErht.setMaxWidth(Double.MAX_VALUE);
        cambiarErht.setOnAction(e -> {
            avisoClaveErht.setVisible(false);
            if (UtilidadesInterfaz.ejecutar(() -> s.usuarios().cambiarClave(u.idUsuario(), actualErht.getText(), nuevaErht.getText(),
                    repetirErht.getText()))) {
                actualErht.clear();
                nuevaErht.clear();
                repetirErht.clear();
                avisoClaveErht.setText("Tu contraseña se cambió.");
                avisoClaveErht.setVisible(true);
            }
        });
        VBox claveErht = new VBox(14, seccionErht("Contraseña", "Usa una que no uses en otros sitios."),
                campoErht("Contraseña actual", actualErht), campoErht("Nueva contraseña", nuevaErht),
                campoErht("Repite la nueva", repetirErht), cambiarErht, avisoClaveErht);
        claveErht.getStyleClass().add("tarjeta-blanca");
        claveErht.setPrefWidth(360);
        claveErht.setMinWidth(320);

        HBox columnasErht = new HBox(22, datosErht, claveErht);
        getChildren().addAll(UtilidadesInterfaz.encabezado("Mi cuenta", "Perfil", "Hola, " + u.nombreVisible() + "."), columnasErht);
    }

    private static VBox seccionErht(String titulo, String texto) {
        return new VBox(4, UtilidadesInterfaz.seccion(titulo), UtilidadesInterfaz.subtitulo(texto));
    }

    private static VBox campoErht(String titulo, javafx.scene.control.Control control) {
        Label l = new Label(UtilidadesInterfaz.espaciado(titulo));
        l.getStyleClass().add("campo-titulo");
        control.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, l, control);
    }

    private static void filaErht(GridPane g, int f, String t1, TextField c1, String t2, TextField c2) {
        VBox a = campoErht(t1, c1);
        VBox b = campoErht(t2, c2);
        GridPane.setHgrow(a, Priority.ALWAYS);
        GridPane.setHgrow(b, Priority.ALWAYS);
        g.add(a, 0, f);
        g.add(b, 1, f);
    }
}
