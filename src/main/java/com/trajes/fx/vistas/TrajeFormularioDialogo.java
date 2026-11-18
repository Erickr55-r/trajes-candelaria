package com.trajes.fx.vistas;

import com.trajes.fx.Servicios;
import com.trajes.fx.Ui;
import com.trajes.modelo.Opcion;
import com.trajes.modelo.Traje;
import com.trajes.servicio.ReglaNegocioException;
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

public class TrajeFormularioDialogo {

    private final Servicios s;
    private final Integer idTraje;
    private Integer guardado;

    public TrajeFormularioDialogo(Servicios s, Integer idTraje) {
        this.s = s;
        this.idTraje = idTraje;
    }

    public Integer mostrar() {
        Ui.ejecutar(this::abrir);
        return guardado;
    }

    private void abrir() {
        Traje t = idTraje == null ? null : s.trajes().buscar(idTraje);
        List<Opcion> danzas = s.trajes().danzas();

        TextField codigo = Ui.campo(t == null ? "" : t.codigo(), "Ej.: DIA-021");
        ComboBox<Opcion> danza = Ui.combo(danzas, Opcion::nombre, null);
        if (t != null) {
            danzas.stream().filter(d -> d.id().equals(t.idDanza())).findFirst().ifPresent(danza::setValue);
        }
        TextField personaje = Ui.campo(t == null ? "" : t.personaje(), "Ej.: Caporal");
        ComboBox<String> genero = Ui.comboTexto(List.of("VARON", "MUJER", "UNISEX"), null);
        ComboBox<String> talla = Ui.comboTexto(Ui.TALLAS, null);
        talla.setValue(t == null ? "M" : t.talla());
        if (t != null) {
            genero.setValue(t.genero());
        }
        TextField color = Ui.campo(t == null ? "" : t.color(), "Opcional");
        TextField precio = Ui.campo(t == null ? "" : Ui.montoEditable(t.precioAlquiler()), "Precio del alquiler S/");
        TextField garantia = Ui.campo(t == null ? "" : Ui.montoEditable(t.garantia()), "Garantía S/");
        DatePicker adquisicion = Ui.fechaPicker(t == null ? null : t.fechaAdquisicion());

        GridPane g = Ui.formulario();
        Ui.fila(g, 0, "Código *", codigo);
        Ui.fila(g, 1, "Danza *", danza);
        Ui.fila(g, 2, "Personaje *", personaje);
        Ui.fila(g, 3, "Género *", genero);
        Ui.fila(g, 4, "Talla *", talla);
        Ui.fila(g, 5, "Color", color);
        Ui.fila(g, 6, "Precio de alquiler (S/) *", precio);
        Ui.fila(g, 7, "Garantía (S/) *", garantia);
        Ui.fila(g, 8, "Fecha de adquisición", adquisicion);

        BorderPane raiz = new BorderPane();
        VBox cabecera = new VBox(4, Ui.titulo(t == null ? "Nuevo traje" : "Editar traje " + t.codigo()),
                Ui.subtitulo("Los campos con * son obligatorios."));
        cabecera.getStyleClass().add("cabecera");
        raiz.setTop(cabecera);
        VBox centro = new VBox(g);
        centro.getStyleClass().add("contenido");
        raiz.setCenter(centro);

        Stage stage = Ui.ventana(t == null ? "Nuevo traje" : "Editar traje", raiz, 520, 560);
        Button cancelar = new Button("Cancelar");
        cancelar.setOnAction(e -> stage.close());
        Button guardar = Ui.estilo(new Button("Guardar"), "boton-primario");
        guardar.setDefaultButton(true);
        guardar.setOnAction(e -> Ui.ejecutar(() -> {
            BigDecimal p = Ui.monto(Ui.requerido(precio, "Precio de alquiler"));
            BigDecimal gar = Ui.monto(Ui.requerido(garantia, "Garantía"));
            if (p.signum() <= 0) {
                throw new ReglaNegocioException("El precio de alquiler debe ser mayor que cero.");
            }
            guardado = s.trajes().guardar(idTraje, Ui.requerido(codigo, "Código"),
                    Ui.requerido(danza, "Danza").id(), Ui.requerido(personaje, "Personaje"),
                    Ui.requerido(genero, "Género"), Ui.requerido(talla, "Talla"), color.getText(),
                    p, gar, Ui.fechaDe(adquisicion));
            stage.close();
            Ui.info("Traje guardado correctamente.");
        }));
        HBox pie = new HBox(cancelar, guardar);
        pie.getStyleClass().add("barra-acciones");
        raiz.setBottom(pie);
        stage.showAndWait();
    }
}
