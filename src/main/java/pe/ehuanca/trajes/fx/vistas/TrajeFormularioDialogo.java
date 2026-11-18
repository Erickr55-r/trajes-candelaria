/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.Opcion;
import pe.ehuanca.trajes.modelo.Traje;
import pe.ehuanca.trajes.servicio.ExcepcionReglaNegocio;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.List;

/**
 * Formulario para registrar o editar un traje.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class TrajeFormularioDialogo {

    private final Servicios sErht;
    private final Integer idTrajeErht;
    private Integer guardadoErht;

    public TrajeFormularioDialogo(Servicios s, Integer idTraje) {
        this.sErht = s;
        this.idTrajeErht = idTraje;
    }

    public Integer mostrar() {
        UtilidadesInterfaz.ejecutar(this::abrirErht);
        return guardadoErht;
    }

    private void abrirErht() {
        Traje t = idTrajeErht == null ? null : sErht.trajes().buscar(idTrajeErht);
        List<Opcion> danzasErht = sErht.trajes().danzas();

        TextField codigoErht = UtilidadesInterfaz.campo(t == null ? "" : t.codigo(), "Ej.: DIA-021");
        ComboBox<Opcion> danzaErht = UtilidadesInterfaz.combo(danzasErht, Opcion::nombre, null);
        if (t != null) {
            danzasErht.stream().filter(d -> d.id().equals(t.idDanza())).findFirst().ifPresent(danzaErht::setValue);
        }
        TextField personajeErht = UtilidadesInterfaz.campo(t == null ? "" : t.personaje(), "Ej.: Caporal");
        ComboBox<String> generoErht = UtilidadesInterfaz.comboTexto(List.of("VARON", "MUJER", "UNISEX"), null);
        ComboBox<String> tallaErht = UtilidadesInterfaz.comboTexto(UtilidadesInterfaz.TALLAS, null);
        tallaErht.setValue(t == null ? "M" : t.talla());
        if (t != null) {
            generoErht.setValue(t.genero());
        }
        TextField colorErht = UtilidadesInterfaz.campo(t == null ? "" : t.color(), "Opcional");
        TextField precioErht = UtilidadesInterfaz.campo(t == null ? "" : UtilidadesInterfaz.montoEditable(t.precioAlquiler()), "Precio del alquiler S/");
        TextField garantiaErht = UtilidadesInterfaz.campo(t == null ? "" : UtilidadesInterfaz.montoEditable(t.garantia()), "Garantía S/");
        DatePicker adquisicionErht = UtilidadesInterfaz.selectorFecha(t == null ? null : t.fechaAdquisicion());

        GridPane g = UtilidadesInterfaz.formulario();
        UtilidadesInterfaz.fila(g, 0, "Código *", codigoErht);
        UtilidadesInterfaz.fila(g, 1, "Danza *", danzaErht);
        UtilidadesInterfaz.fila(g, 2, "Personaje *", personajeErht);
        UtilidadesInterfaz.fila(g, 3, "Género *", generoErht);
        UtilidadesInterfaz.fila(g, 4, "Talla *", tallaErht);
        UtilidadesInterfaz.fila(g, 5, "Color", colorErht);
        UtilidadesInterfaz.fila(g, 6, "Precio de alquiler (S/) *", precioErht);
        UtilidadesInterfaz.fila(g, 7, "Garantía (S/) *", garantiaErht);
        UtilidadesInterfaz.fila(g, 8, "Fecha de adquisición", adquisicionErht);

        BorderPane raizErht = new BorderPane();
        VBox cabeceraErht = new VBox(4, UtilidadesInterfaz.titulo(t == null ? "Nuevo traje" : "Editar traje " + t.codigo()),
                UtilidadesInterfaz.subtitulo("Los campos con * son obligatorios."));
        cabeceraErht.getStyleClass().add("cabecera");
        raizErht.setTop(cabeceraErht);
        VBox centroErht = new VBox(g);
        centroErht.getStyleClass().add("contenido");
        raizErht.setCenter(centroErht);

        Stage ventanaDialogoErht = UtilidadesInterfaz.ventana(t == null ? "Nuevo traje" : "Editar traje", raizErht, 520, 560);
        Button cancelarErht = new Button("Cancelar");
        cancelarErht.setOnAction(e -> ventanaDialogoErht.close());
        Button guardarErht = UtilidadesInterfaz.estilo(new Button("Guardar"), "boton-primario");
        guardarErht.setDefaultButton(true);
        guardarErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(() -> {
            BigDecimal p = UtilidadesInterfaz.monto(UtilidadesInterfaz.requerido(precioErht, "Precio de alquiler"));
            BigDecimal garErht = UtilidadesInterfaz.monto(UtilidadesInterfaz.requerido(garantiaErht, "Garantía"));
            if (p.signum() <= 0) {
                throw new ExcepcionReglaNegocio("El precio de alquiler debe ser mayor que cero.");
            }
            guardadoErht = sErht.trajes().guardar(idTrajeErht, UtilidadesInterfaz.requerido(codigoErht, "Código"),
                    UtilidadesInterfaz.requerido(danzaErht, "Danza").id(), UtilidadesInterfaz.requerido(personajeErht, "Personaje"),
                    UtilidadesInterfaz.requerido(generoErht, "Género"), UtilidadesInterfaz.requerido(tallaErht, "Talla"), colorErht.getText(),
                    p, garErht, UtilidadesInterfaz.fechaDe(adquisicionErht));
            ventanaDialogoErht.close();
            UtilidadesInterfaz.mostrarInformacion("Traje guardado correctamente.");
        }));
        HBox pieErht = new HBox(cancelarErht, guardarErht);
        pieErht.getStyleClass().add("barra-acciones");
        raizErht.setBottom(pieErht);
        ventanaDialogoErht.showAndWait();
    }
}
