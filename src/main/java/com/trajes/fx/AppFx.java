package com.trajes.fx;

import com.trajes.AlquilerTrajesApplication;
import com.trajes.servicio.AlquilerService;
import com.trajes.servicio.ClienteService;
import com.trajes.servicio.PersonalizadorService;
import com.trajes.servicio.ReporteService;
import com.trajes.servicio.TrajeService;
import com.trajes.servicio.UsuarioService;
import com.trajes.modelo.Usuario;
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

public class AppFx extends Application {

    private ConfigurableApplicationContext contexto;
    private Exception errorInicio;
    private Servicios servicios;
    private Stage stage;
    private Scene escena;

    @Override
    public void init() {
        try {
            contexto = new SpringApplicationBuilder(AlquilerTrajesApplication.class)
                    .web(WebApplicationType.NONE)
                    .run(getParameters().getRaw().toArray(new String[0]));
            
            contexto.getBean(JdbcTemplate.class).queryForObject("SELECT COUNT(*) FROM traje", Long.class);
        } catch (Exception e) {
            errorInicio = e;
        }
    }

    @Override
    public void start(Stage stage) {
        Fuentes.cargar();
        if (errorInicio != null) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Alquiler de trajes");
            alerta.setHeaderText("No se pudo conectar a PostgreSQL");
            alerta.setContentText(Ui.primeraLinea(errorInicio) + "\n\n"
                    + "Revisa que la variable de entorno DB_PASSWORD tenga la contraseña de PostgreSQL "
                    + "(en IntelliJ: Run Configuration «Trajes Candelaria»; ver application-example.properties), "
                    + "que PostgreSQL esté encendido y que exista la base alquiler_trajes cargada con "
                    + "db/base_datos.sql.");
            alerta.getDialogPane().getStylesheets().add(Ui.CSS);
            alerta.getDialogPane().setPrefWidth(560);
            alerta.showAndWait();
            Platform.exit();
            return;
        }
        servicios = new Servicios(contexto.getBean(AlquilerService.class),
                contexto.getBean(TrajeService.class), contexto.getBean(ClienteService.class),
                contexto.getBean(ReporteService.class), contexto.getBean(PersonalizadorService.class),
                contexto.getBean(UsuarioService.class));

        this.stage = stage;
        escena = new Scene(new Group(), 1360, 840);
        escena.getStylesheets().add(Ui.CSS);
        stage.setTitle("Trajes Candelaria · Alquiler y confección");
        stage.setMinWidth(1024);
        stage.setMinHeight(640);
        stage.setScene(escena);
        mostrarLogin();
        stage.show();
    }

    public void mostrarLogin() {
        Sesion.get().cerrar();
        escena.setRoot(new LoginView(servicios, this::ingresar));
        stage.setTitle("Trajes Candelaria · Iniciar sesión");
    }

    public void ingresar(Usuario u) {
        Sesion.get().iniciar(u);
        Parent raiz = u.esCliente()
                ? new PortalClienteView(servicios, this::mostrarLogin)
                : new PanelVendedorView(servicios, this::mostrarLogin);
        escena.setRoot(raiz);
        stage.setTitle("Trajes Candelaria · " + u.nombreVisible() + " (" + u.rolVisible() + ")");
    }

    @Override
    public void stop() {
        if (contexto != null) {
            contexto.close();
        }
    }
}
