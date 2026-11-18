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

    private ConfigurableApplicationContext contexto;
    private Exception errorInicio;
    private Servicios servicios;
    private Stage escenario;
    private Scene escena;

    @Override
    public void init() {
        try {
            contexto = new SpringApplicationBuilder(AplicacionAlquilerTrajes.class)
                    .web(WebApplicationType.NONE)
                    .run(getParameters().getRaw().toArray(new String[0]));
            // Prueba la conexión (y que exista la tabla) antes de abrir la ventana.
            contexto.getBean(JdbcTemplate.class).queryForObject("SELECT COUNT(*) FROM traje", Long.class);
        } catch (Exception e) {
            errorInicio = e;
        }
    }

    @Override
    public void start(Stage escenario) {
        Fuentes.cargar();
        if (errorInicio != null) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Alquiler de trajes");
            alerta.setHeaderText("No se pudo conectar a PostgreSQL");
            alerta.setContentText(UtilidadesInterfaz.primeraLinea(errorInicio) + "\n\n"
                    + "Revisa que la variable de entorno DB_PASSWORD tenga la contraseña de PostgreSQL "
                    + "(en IntelliJ: Run Configuration «Trajes Candelaria»; ver application-example.properties), "
                    + "que PostgreSQL esté encendido y que exista la base alquiler_trajes cargada con "
                    + "db/base_datos.sql.");
            alerta.getDialogPane().getStylesheets().add(UtilidadesInterfaz.HOJA_ESTILOS);
            alerta.getDialogPane().setPrefWidth(560);
            alerta.showAndWait();
            Platform.exit();
            return;
        }
        servicios = new Servicios(contexto.getBean(AlquilerServicio.class),
                contexto.getBean(TrajeServicio.class), contexto.getBean(ClienteServicio.class),
                contexto.getBean(ReporteServicio.class), contexto.getBean(PersonalizadorServicio.class),
                contexto.getBean(UsuarioServicio.class));

        this.escenario = escenario;
        escena = new Scene(new Group(), 1360, 840);
        escena.getStylesheets().add(UtilidadesInterfaz.HOJA_ESTILOS);
        escenario.setTitle("Trajes Candelaria · Alquiler y confección");
        escenario.setMinWidth(1024);
        escenario.setMinHeight(640);
        escenario.setScene(escena);
        mostrarInicioSesion();
        escenario.show();
    }

    /** Pantalla de inicio de sesión (también al cerrar sesión). */
    public void mostrarInicioSesion() {
        Sesion.actual().cerrar();
        escena.setRoot(new VistaInicioSesion(servicios, this::ingresar));
        escenario.setTitle("Trajes Candelaria · Iniciar sesión");
    }

    /** Guarda la sesión y abre la pantalla de su rol: CLIENTE → portal; ADMIN o VENDEDOR → panel de la tienda. */
    public void ingresar(Usuario u) {
        Sesion.actual().iniciar(u);
        Parent raiz = u.esCliente()
                ? new VistaPortalCliente(servicios, this::mostrarInicioSesion)
                : new VistaPanelVendedor(servicios, this::mostrarInicioSesion);
        escena.setRoot(raiz);
        escenario.setTitle("Trajes Candelaria · " + u.nombreVisible() + " (" + u.rolVisible() + ")");
    }

    @Override
    public void stop() {
        if (contexto != null) {
            contexto.close();
        }
    }
}
