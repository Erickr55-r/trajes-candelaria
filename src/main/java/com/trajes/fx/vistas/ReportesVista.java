package com.trajes.fx.vistas;

import com.trajes.fx.Servicios;
import com.trajes.fx.Ui;
import com.trajes.modelo.Tabla;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** Reportes del negocio: una pestaña por cada consulta. */
public class ReportesVista extends VBox {

    public ReportesVista(Servicios s) {
        if (!com.trajes.modelo.Usuario.ADMIN.equals(com.trajes.fx.Sesion.get().usuario().rol())) {
            throw new IllegalStateException("Solo el ADMIN puede ver los reportes.");
        }
        getStyleClass().add("contenido");

        TabPane pestanias = new TabPane();
        pestanias.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        for (Tabla t : s.reportes().todos()) {
            pestanias.getTabs().add(new Tab(t.titulo(), contenido(t)));
        }
        VBox.setVgrow(pestanias, Priority.ALWAYS);

        pestanias.setMinHeight(480);
        getChildren().addAll(Ui.encabezado("Indicadores", "Reportes",
                "Consultas de gestión calculadas directamente en PostgreSQL."), pestanias);
    }

    private VBox contenido(Tabla t) {
        TableView<List<Object>> tabla = Ui.tabla("Todavía no hay datos para este reporte.");
        for (int j = 0; j < t.columnas().size(); j++) {
            tabla.getColumns().add(columna(t.columnas().get(j), j, t.filas()));
        }
        tabla.getItems().setAll(t.filas());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        VBox caja = new VBox(10, Ui.seccion(t.titulo()), Ui.subtitulo(t.descripcion()), tabla);
        caja.setPadding(new javafx.geometry.Insets(6, 0, 0, 0));
        return caja;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private TableColumn<List<Object>, Object> columna(String titulo, int indice, List<List<Object>> filas) {
        TableColumn<List<Object>, Object> col = new TableColumn<>(Ui.cabeceraColumna(titulo));
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().get(indice)));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : Ui.texto(item));
            }
        });
        boolean numerica = !filas.isEmpty() && filas.get(0).get(indice) instanceof Number;
        if (numerica) {
            col.setStyle("-fx-alignment: CENTER-RIGHT;");
        }
        col.setComparator((a, b) -> {
            if (a == null || b == null) {
                return a == null ? (b == null ? 0 : -1) : 1;
            }
            if (a instanceof Number na && b instanceof Number nb) {
                return Double.compare(na.doubleValue(), nb.doubleValue());
            }
            if (a instanceof Comparable ca && a.getClass() == b.getClass()) {
                return ca.compareTo(b);
            }
            return a.toString().compareTo(b.toString());
        });
        return col;
    }
}
