/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.Tabla;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Reportes del negocio: una pestaña por cada consulta.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class ReportesVista extends VBox {

    public ReportesVista(Servicios s) {
        if (!pe.ehuanca.trajes.modelo.Usuario.ADMIN.equals(pe.ehuanca.trajes.fx.Sesion.actual().usuario().rol())) {
            throw new IllegalStateException("Solo el ADMIN puede ver los reportes.");
        }
        getStyleClass().add("contenido");

        TabPane pestaniasErht = new TabPane();
        pestaniasErht.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        for (Tabla t : s.reportes().todos()) {
            pestaniasErht.getTabs().add(new Tab(t.titulo(), contenidoErht(t)));
        }
        VBox.setVgrow(pestaniasErht, Priority.ALWAYS);

        pestaniasErht.setMinHeight(480);
        getChildren().addAll(UtilidadesInterfaz.encabezado("Indicadores", "Reportes",
                "Consultas de gestión calculadas directamente en PostgreSQL."), pestaniasErht);
    }

    private VBox contenidoErht(Tabla t) {
        TableView<List<Object>> tablaErht = UtilidadesInterfaz.tabla("Todavía no hay datos para este reporte.");
        for (int j = 0; j < t.columnas().size(); j++) {
            tablaErht.getColumns().add(columnaErht(t.columnas().get(j), j, t.filas()));
        }
        tablaErht.getItems().setAll(t.filas());
        VBox.setVgrow(tablaErht, Priority.ALWAYS);
        VBox cajaErht = new VBox(10, UtilidadesInterfaz.seccion(t.titulo()), UtilidadesInterfaz.subtitulo(t.descripcion()), tablaErht);
        cajaErht.setPadding(new javafx.geometry.Insets(6, 0, 0, 0));
        return cajaErht;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private TableColumn<List<Object>, Object> columnaErht(String titulo, int indice, List<List<Object>> filas) {
        TableColumn<List<Object>, Object> colErht = new TableColumn<>(UtilidadesInterfaz.cabeceraColumna(titulo));
        colErht.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().get(indice)));
        colErht.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : UtilidadesInterfaz.texto(item));
            }
        });
        boolean numericaErht = !filas.isEmpty() && filas.get(0).get(indice) instanceof Number;
        if (numericaErht) {
            colErht.setStyle("-fx-alignment: CENTER-RIGHT;");
        }
        colErht.setComparator((a, b) -> {
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
        return colErht;
    }
}
