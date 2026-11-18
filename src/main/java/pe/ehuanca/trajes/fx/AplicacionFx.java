/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx;

import pe.ehuanca.trajes.AplicacionAlquilerTrajes;
import pe.ehuanca.trajes.servicio.AlquilerServicio;
import pe.ehuanca.trajes.servicio.ClienteServicio;
import pe.ehuanca.trajes.servicio.PersonalizadorServicio;
import pe.ehuanca.trajes.servicio.ReporteServicio;
import pe.ehuanca.trajes.servicio.TrajeServicio;
import pe.ehuanca.trajes.servicio.UsuarioServicio;
import pe.ehuanca.trajes.modelo.Usuario;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Aplicación JavaFX. En init() arranca Spring (conexión a PostgreSQL y servicios),
 * en start() abre la ventana y en stop() cierra Spring.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class AplicacionFx extends Application {

    private ConfigurableApplicationContext contextoErht;
    private Exception errorInicioErht;
    private Servicios serviciosErht;
    private Stage escenarioErht;
    private Scene escenaErht;

    @Override
    public void init() {
        try {
            contextoErht = new SpringApplicationBuilder(AplicacionAlquilerTrajes.class)
                    .web(WebApplicationType.NONE)
                    .run(getParameters().getRaw().toArray(new String[0]));
            contextoErht.getBean(JdbcTemplate.class).queryForObject("SELECT COUNT(*) FROM traje", Long.class);
        } catch (Exception e) {
            errorInicioErht = e;
        }
    }

    @Override
    public void start(Stage escenario) {
        Fuentes.cargar();
        if (errorInicioErht != null) {
            Alert alertaErht = new Alert(Alert.AlertType.ERROR);
            alertaErht.setTitle("Alquiler de trajes");
            alertaErht.setHeaderText("No se pudo conectar a PostgreSQL");
            alertaErht.setContentText(UtilidadesInterfaz.primeraLinea(errorInicioErht) + "\n\n"
                    + "Revisa que la variable de entorno DB_PASSWORD tenga la contraseña de PostgreSQL "
                    + "(en IntelliJ: Run Configuration «Trajes Candelaria»; ver application-example.properties), "
                    + "que PostgreSQL esté encendido y que exista la base alquiler_trajes cargada con "
                    + "db/base_datos.sql.");
            alertaErht.getDialogPane().getStylesheets().add(UtilidadesInterfaz.HOJA_ESTILOS);
            alertaErht.getDialogPane().setPrefWidth(560);
            alertaErht.showAndWait();
            Platform.exit();
            return;
        }
        serviciosErht = new Servicios(contextoErht.getBean(AlquilerServicio.class),
                contextoErht.getBean(TrajeServicio.class), contextoErht.getBean(ClienteServicio.class),
                contextoErht.getBean(ReporteServicio.class), contextoErht.getBean(PersonalizadorServicio.class),
                contextoErht.getBean(UsuarioServicio.class));

        this.escenarioErht = escenario;
        escenaErht = new Scene(new Group(), 1360, 840);
        escenaErht.getStylesheets().add(UtilidadesInterfaz.HOJA_ESTILOS);
        escenario.setTitle("Trajes Candelaria · Alquiler y confección");
        escenario.setMinWidth(1024);
        escenario.setMinHeight(640);
        escenario.setScene(escenaErht);
        mostrarInicioSesion();
        escenario.show();
    }

    public void mostrarInicioSesion() {
        Sesion.actual().cerrar();
        escenaErht.setRoot(new VistaInicioSesion(serviciosErht, this::ingresar));
        escenarioErht.setTitle("Trajes Candelaria · Iniciar sesión");
    }

    public void ingresar(Usuario u) {
        Sesion.actual().iniciar(u);
        Parent raizErht = u.esCliente()
                ? new VistaPortalCliente(serviciosErht, this::mostrarInicioSesion)
                : new VistaPanelVendedor(serviciosErht, this::mostrarInicioSesion);
        escenaErht.setRoot(raizErht);
        escenarioErht.setTitle("Trajes Candelaria · " + u.nombreVisible() + " (" + u.rolVisible() + ")");
    }

    @Override
    public void stop() {
        if (contextoErht != null) {
            contextoErht.close();
        }
    }
}
