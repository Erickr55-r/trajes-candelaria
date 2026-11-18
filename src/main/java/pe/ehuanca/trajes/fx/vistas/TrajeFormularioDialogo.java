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

    private final Servicios s;
    private final Integer idTraje;
    private Integer guardado;

    public TrajeFormularioDialogo(Servicios s, Integer idTraje) {
        this.s = s;
        this.idTraje = idTraje;
    }

    /** Muestra el formulario; devuelve el id del traje guardado o null si se canceló. */
    public Integer mostrar() {
        UtilidadesInterfaz.ejecutar(this::abrir);
        return guardado;
    }

    private void abrir() {
        Traje t = idTraje == null ? null : s.trajes().buscar(idTraje);
        List<Opcion> danzas = s.trajes().danzas();

        TextField codigo = UtilidadesInterfaz.campo(t == null ? "" : t.codigo(), "Ej.: DIA-021");
        ComboBox<Opcion> danza = UtilidadesInterfaz.combo(danzas, Opcion::nombre, null);
        if (t != null) {
            danzas.stream().filter(d -> d.id().equals(t.idDanza())).findFirst().ifPresent(danza::setValue);
        }
        TextField personaje = UtilidadesInterfaz.campo(t == null ? "" : t.personaje(), "Ej.: Caporal");
        ComboBox<String> genero = UtilidadesInterfaz.comboTexto(List.of("VARON", "MUJER", "UNISEX"), null);
        ComboBox<String> talla = UtilidadesInterfaz.comboTexto(UtilidadesInterfaz.TALLAS, null);
        talla.setValue(t == null ? "M" : t.talla());
        if (t != null) {
            genero.setValue(t.genero());
        }
        TextField color = UtilidadesInterfaz.campo(t == null ? "" : t.color(), "Opcional");
        TextField precio = UtilidadesInterfaz.campo(t == null ? "" : UtilidadesInterfaz.montoEditable(t.precioAlquiler()), "Precio del alquiler S/");
        TextField garantia = UtilidadesInterfaz.campo(t == null ? "" : UtilidadesInterfaz.montoEditable(t.garantia()), "Garantía S/");
        DatePicker adquisicion = UtilidadesInterfaz.selectorFecha(t == null ? null : t.fechaAdquisicion());

        GridPane g = UtilidadesInterfaz.formulario();
        UtilidadesInterfaz.fila(g, 0, "Código *", codigo);
        UtilidadesInterfaz.fila(g, 1, "Danza *", danza);
        UtilidadesInterfaz.fila(g, 2, "Personaje *", personaje);
        UtilidadesInterfaz.fila(g, 3, "Género *", genero);
        UtilidadesInterfaz.fila(g, 4, "Talla *", talla);
        UtilidadesInterfaz.fila(g, 5, "Color", color);
        UtilidadesInterfaz.fila(g, 6, "Precio de alquiler (S/) *", precio);
        UtilidadesInterfaz.fila(g, 7, "Garantía (S/) *", garantia);
        UtilidadesInterfaz.fila(g, 8, "Fecha de adquisición", adquisicion);

        BorderPane raiz = new BorderPane();
        VBox cabecera = new VBox(4, UtilidadesInterfaz.titulo(t == null ? "Nuevo traje" : "Editar traje " + t.codigo()),
                UtilidadesInterfaz.subtitulo("Los campos con * son obligatorios."));
        cabecera.getStyleClass().add("cabecera");
        raiz.setTop(cabecera);
        VBox centro = new VBox(g);
        centro.getStyleClass().add("contenido");
        raiz.setCenter(centro);

        Stage ventanaDialogo = UtilidadesInterfaz.ventana(t == null ? "Nuevo traje" : "Editar traje", raiz, 520, 560);
        Button cancelar = new Button("Cancelar");
        cancelar.setOnAction(e -> ventanaDialogo.close());
        Button guardar = UtilidadesInterfaz.estilo(new Button("Guardar"), "boton-primario");
        guardar.setDefaultButton(true);
        guardar.setOnAction(e -> UtilidadesInterfaz.ejecutar(() -> {
            BigDecimal p = UtilidadesInterfaz.monto(UtilidadesInterfaz.requerido(precio, "Precio de alquiler"));
            BigDecimal gar = UtilidadesInterfaz.monto(UtilidadesInterfaz.requerido(garantia, "Garantía"));
            if (p.signum() <= 0) {
                throw new ExcepcionReglaNegocio("El precio de alquiler debe ser mayor que cero.");
            }
            guardado = s.trajes().guardar(idTraje, UtilidadesInterfaz.requerido(codigo, "Código"),
                    UtilidadesInterfaz.requerido(danza, "Danza").id(), UtilidadesInterfaz.requerido(personaje, "Personaje"),
                    UtilidadesInterfaz.requerido(genero, "Género"), UtilidadesInterfaz.requerido(talla, "Talla"), color.getText(),
                    p, gar, UtilidadesInterfaz.fechaDe(adquisicion));
            ventanaDialogo.close();
            UtilidadesInterfaz.mostrarInformacion("Traje guardado correctamente.");
        }));
        HBox pie = new HBox(cancelar, guardar);
        pie.getStyleClass().add("barra-acciones");
        raiz.setBottom(pie);
        ventanaDialogo.showAndWait();
    }
}
