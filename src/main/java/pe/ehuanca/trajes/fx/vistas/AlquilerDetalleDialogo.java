/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.AlquilerDetalleTraje;
import pe.ehuanca.trajes.modelo.AlquilerResumen;
import pe.ehuanca.trajes.modelo.DanioFila;
import pe.ehuanca.trajes.modelo.PagoFila;
import pe.ehuanca.trajes.modelo.PiezaAlquiler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Detalle de un alquiler: montos, trajes, pagos, daños y las acciones según su estado.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class AlquilerDetalleDialogo {

    private static final int FILAS_DANIO = 4;

    private final Servicios sErht;
    private final int idAlquilerErht;
    private final BorderPane raizErht = new BorderPane();
    private Stage ventanaDialogoErht;

    public AlquilerDetalleDialogo(Servicios s, int idAlquiler) {
        this.sErht = s;
        this.idAlquilerErht = idAlquiler;
    }

    public void mostrar() {
        if (!UtilidadesInterfaz.ejecutar(this::cargarErht)) {
            return;
        }
        ventanaDialogoErht = UtilidadesInterfaz.ventana("Alquiler N° " + idAlquilerErht, raizErht, 1080, 760);
        ventanaDialogoErht.showAndWait();
    }

    private void cargarErht() {
        AlquilerResumen a = sErht.alquileres().buscar(idAlquilerErht);
        List<AlquilerDetalleTraje> trajesErht = sErht.alquileres().trajes(idAlquilerErht);
        List<PagoFila> pagosErht = sErht.alquileres().pagos(idAlquilerErht);
        List<DanioFila> daniosErht = sErht.alquileres().danios(idAlquilerErht);

        raizErht.setTop(cabeceraErht(a));

        HBox tarjetasErht = new HBox(12,
                UtilidadesInterfaz.tarjeta("Monto del alquiler", UtilidadesInterfaz.soles(a.montoAlquiler()), "granate"),
                UtilidadesInterfaz.tarjeta("Pagado del alquiler", UtilidadesInterfaz.soles(a.pagadoAlquiler()), "verde"),
                UtilidadesInterfaz.tarjeta("Saldo pendiente", UtilidadesInterfaz.soles(a.saldo()), a.saldo().signum() > 0 ? "naranja" : "verde"),
                UtilidadesInterfaz.tarjeta("Garantía", UtilidadesInterfaz.soles(a.montoGarantia()), "dorado"),
                UtilidadesInterfaz.tarjeta("Daños", UtilidadesInterfaz.soles(a.montoDanios()), a.montoDanios().signum() > 0 ? "rojo" : "gris"));

        VBox cuerpoErht = new VBox(16, tarjetasErht);
        cuerpoErht.getStyleClass().add("contenido");
        switch (a.estado()) {
            case "SOLICITADO" -> cuerpoErht.getChildren().add(accionesSolicitudErht(a));
            case "RECHAZADO" -> cuerpoErht.getChildren().add(UtilidadesInterfaz.panel(UtilidadesInterfaz.seccion("Solicitud rechazada"),
                    UtilidadesInterfaz.subtitulo("Motivo: " + UtilidadesInterfaz.texto(sErht.alquileres().motivoRechazo(idAlquilerErht)))));
            case "RESERVADO" -> cuerpoErht.getChildren().add(accionesReservaErht(a));
            case "ENTREGADO" -> cuerpoErht.getChildren().add(devolucionErht(a));
            default -> { }
        }
        cuerpoErht.getChildren().addAll(
                UtilidadesInterfaz.seccion("Trajes (" + trajesErht.size() + ")"), tablaTrajesErht(trajesErht),
                new HBox(16, columnaErht("Pagos", tablaPagosErht(pagosErht)), columnaErht("Daños registrados", tablaDaniosErht(daniosErht))));

        ScrollPane desplazableErht = new ScrollPane(cuerpoErht);
        desplazableErht.setFitToWidth(true);
        raizErht.setCenter(desplazableErht);

        Button cerrarErht = new Button("Cerrar");
        cerrarErht.setOnAction(e -> ventanaDialogoErht.close());
        HBox pieErht = new HBox(cerrarErht);
        pieErht.getStyleClass().add("barra-acciones");
        raizErht.setBottom(pieErht);
    }

    private VBox cabeceraErht(AlquilerResumen a) {
        HBox lineaErht = new HBox(12, UtilidadesInterfaz.titulo("Alquiler N° " + a.idAlquiler()), UtilidadesInterfaz.insigniasAlquiler(a));
        lineaErht.setAlignment(Pos.CENTER_LEFT);
        VBox cajaErht = new VBox(4, lineaErht,
                UtilidadesInterfaz.subtitulo("Cliente: " + a.cliente()
                        + (a.conjunto() != null ? "   ·   Conjunto: " + a.conjunto() : "")),
                UtilidadesInterfaz.subtitulo("Salida: " + UtilidadesInterfaz.fecha(a.fechaSalida())
                        + "   ·   Devolución prevista: " + UtilidadesInterfaz.fecha(a.fechaDevolucionPrevista())
                        + (a.fechaDevolucionReal() != null ? "   ·   Devuelto: " + UtilidadesInterfaz.fecha(a.fechaDevolucionReal()) : "")
                        + "   ·   " + a.numTrajes() + " traje(s)"));
        if (a.observacion() != null && !a.observacion().isBlank()) {
            cajaErht.getChildren().add(UtilidadesInterfaz.subtitulo("Observación: " + a.observacion()));
        }
        cajaErht.getStyleClass().add("cabecera");
        return cajaErht;
    }

    private VBox accionesSolicitudErht(AlquilerResumen a) {
        Button aprobarErht = UtilidadesInterfaz.estilo(new Button("Aprobar solicitud"), "boton-primario");
        aprobarErht.setOnAction(e -> {
            if (UtilidadesInterfaz.confirmar("La solicitud N° " + idAlquilerErht + " de " + a.cliente() + " pasará a RESERVADO.\n"
                    + "Después podrás registrar el adelanto y la entrega. ¿Aprobar?")) {
                hacerErht(() -> sErht.alquileres().aprobar(idAlquilerErht), "Solicitud aprobada: ahora es una reserva.");
            }
        });
        TextField motivoErht = UtilidadesInterfaz.campo("", "Motivo del rechazo (lo verá el cliente)");
        HBox.setHgrow(motivoErht, Priority.ALWAYS);
        Button rechazarErht = UtilidadesInterfaz.estilo(new Button("Rechazar"), "boton-peligro");
        rechazarErht.setOnAction(e -> {
            if (UtilidadesInterfaz.confirmar("¿Rechazar la solicitud N° " + idAlquilerErht + "? Los trajes quedarán libres.")) {
                hacerErht(() -> sErht.alquileres().rechazar(idAlquilerErht, motivoErht.getText()), "Solicitud rechazada.");
            }
        });
        HBox fila1Erht = new HBox(10, aprobarErht);
        fila1Erht.setAlignment(Pos.CENTER_LEFT);
        HBox fila2Erht = new HBox(10, UtilidadesInterfaz.etiqueta("Motivo"), motivoErht, rechazarErht);
        fila2Erht.setAlignment(Pos.CENTER_LEFT);
        return UtilidadesInterfaz.panel(UtilidadesInterfaz.seccion("Solicitud del cliente por aprobar"),
                UtilidadesInterfaz.subtitulo("El cliente pidió estos trajes desde su portal. Mientras esté SOLICITADO, nadie más puede "
                        + "pedirlos en esas fechas."),
                fila1Erht, fila2Erht);
    }

    private VBox accionesReservaErht(AlquilerResumen a) {
        ComboBox<String> metodoErht = UtilidadesInterfaz.comboTexto(UtilidadesInterfaz.METODOS, null);
        metodoErht.setPrefWidth(170);

        Button entregarErht = UtilidadesInterfaz.estilo(new Button("Registrar entrega"), "boton-primario");
        entregarErht.setOnAction(e -> {
            String m = metodoErht.getValue();
            if (UtilidadesInterfaz.confirmar("Se entregarán los trajes y se cobrará con " + m + ":\n\n"
                    + "  · Saldo del alquiler: " + UtilidadesInterfaz.soles(a.saldo()) + "\n"
                    + "  · Garantía: " + UtilidadesInterfaz.soles(a.montoGarantia()) + "\n\n¿Registrar la entrega?")) {
                hacerErht(() -> sErht.alquileres().entregar(idAlquilerErht, m), "Entrega registrada.");
            }
        });

        TextField adelantoErht = UtilidadesInterfaz.campo("", "Monto S/");
        adelantoErht.setPrefWidth(120);
        Button pagarErht = UtilidadesInterfaz.estilo(new Button("Registrar adelanto"), "boton-dorado");
        pagarErht.setOnAction(e -> hacerErht(() -> sErht.alquileres().registrarPago(idAlquilerErht, "ADELANTO",
                UtilidadesInterfaz.monto(adelantoErht.getText()), metodoErht.getValue()), "Adelanto registrado."));

        Button anularErht = UtilidadesInterfaz.estilo(new Button("Anular alquiler"), "boton-peligro");
        anularErht.setOnAction(e -> {
            if (UtilidadesInterfaz.confirmar("¿Seguro que deseas ANULAR el alquiler N° " + idAlquilerErht + "?\n"
                    + "Los trajes quedarán libres para esas fechas. Esta acción no se puede deshacer.")) {
                hacerErht(() -> sErht.alquileres().anular(idAlquilerErht), "Alquiler anulado.");
            }
        });

        HBox fila1Erht = new HBox(10, UtilidadesInterfaz.etiqueta("Método de pago"), metodoErht, entregarErht, UtilidadesInterfaz.espacio(), anularErht);
        fila1Erht.setAlignment(Pos.CENTER_LEFT);
        HBox fila2Erht = new HBox(10, UtilidadesInterfaz.etiqueta("Adelanto"), adelantoErht, pagarErht);
        fila2Erht.setAlignment(Pos.CENTER_LEFT);
        return UtilidadesInterfaz.panel(UtilidadesInterfaz.seccion("Acciones de la reserva"),
                UtilidadesInterfaz.subtitulo("Al entregar se cobra el saldo pendiente y la garantía con el método elegido."),
                fila1Erht, fila2Erht);
    }

    private VBox devolucionErht(AlquilerResumen a) {
        DatePicker fechaErht = UtilidadesInterfaz.selectorFecha(LocalDate.now());
        fechaErht.setMaxWidth(160);
        ComboBox<String> metodoErht = UtilidadesInterfaz.comboTexto(UtilidadesInterfaz.METODOS, null);
        metodoErht.setPrefWidth(170);

        List<PiezaAlquiler> piezasErht = sErht.alquileres().piezasDelAlquiler(idAlquilerErht);
        GridPane rejillaErht = new GridPane();
        rejillaErht.setHgap(10);
        rejillaErht.setVgap(8);
        rejillaErht.addRow(0, UtilidadesInterfaz.etiqueta("Pieza dañada"), UtilidadesInterfaz.etiqueta("Tipo"), UtilidadesInterfaz.etiqueta("Descripción"),
                UtilidadesInterfaz.etiqueta("Monto S/ (vacío = automático)"));
        List<ComboBox<PiezaAlquiler>> cPiezasErht = new ArrayList<>();
        List<ComboBox<String>> cTiposErht = new ArrayList<>();
        List<TextField> cDescripcionesErht = new ArrayList<>();
        List<TextField> cMontosErht = new ArrayList<>();
        for (int k = 0; k < FILAS_DANIO; k++) {
            ComboBox<PiezaAlquiler> piezaErht = UtilidadesInterfaz.combo(piezasErht,
                    p -> p.etiqueta() + " · repos. " + UtilidadesInterfaz.soles(p.valorReposicion()), "— Sin daño —");
            piezaErht.setPrefWidth(470);
            ComboBox<String> tipoErht = UtilidadesInterfaz.comboTexto(List.of("LEVE", "GRAVE", "PERDIDA"), null);
            tipoErht.setPrefWidth(110);
            TextField descripcionErht = UtilidadesInterfaz.campo("", "Ej.: costura rota");
            TextField montoErht = UtilidadesInterfaz.campo("", "Automático");
            montoErht.setPrefWidth(130);
            rejillaErht.addRow(k + 1, piezaErht, tipoErht, descripcionErht, montoErht);
            GridPane.setHgrow(descripcionErht, Priority.ALWAYS);
            cPiezasErht.add(piezaErht);
            cTiposErht.add(tipoErht);
            cDescripcionesErht.add(descripcionErht);
            cMontosErht.add(montoErht);
        }

        Button registrarErht = UtilidadesInterfaz.estilo(new Button("Registrar devolución"), "boton-primario");
        registrarErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(() -> {
            LocalDate fechaRealErht = UtilidadesInterfaz.requerido(fechaErht, "Fecha de devolución");
            String[] idPiezasErht = new String[FILAS_DANIO];
            String[] tiposErht = new String[FILAS_DANIO];
            String[] descripcionesErht = new String[FILAS_DANIO];
            String[] montosErht = new String[FILAS_DANIO];
            int conDanioErht = 0;
            for (int k = 0; k < FILAS_DANIO; k++) {
                PiezaAlquiler p = cPiezasErht.get(k).getValue();
                idPiezasErht[k] = p == null ? "" : String.valueOf(p.idPieza());
                tiposErht[k] = cTiposErht.get(k).getValue();
                descripcionesErht[k] = cDescripcionesErht.get(k).getText();
                BigDecimal m = UtilidadesInterfaz.monto(cMontosErht.get(k).getText());
                montosErht[k] = m == null ? "" : m.toPlainString();
                if (p != null) {
                    conDanioErht++;
                }
            }
            if (!UtilidadesInterfaz.confirmar("Se registrará la devolución del " + UtilidadesInterfaz.fecha(fechaRealErht) + " con "
                    + (conDanioErht == 0 ? "ningún daño" : conDanioErht + " pieza(s) dañada(s)")
                    + ".\nLa garantía se liquida con " + metodoErht.getValue() + ". ¿Continuar?")) {
                return;
            }
            BigDecimal totalDaniosErht = sErht.alquileres().devolver(idAlquilerErht, fechaRealErht, idPiezasErht, tiposErht,
                    descripcionesErht, montosErht, metodoErht.getValue());
            BigDecimal diferenciaErht = a.montoGarantia().subtract(totalDaniosErht);
            String liquidacionErht = diferenciaErht.signum() > 0
                    ? "Se devuelve al cliente " + UtilidadesInterfaz.soles(diferenciaErht) + " de la garantía."
                    : diferenciaErht.signum() < 0
                    ? "Los daños superan la garantía: se cobró " + UtilidadesInterfaz.soles(diferenciaErht.negate()) + "."
                    : "La garantía cubre exactamente los daños.";
            cargarErht();
            UtilidadesInterfaz.mostrarInformacion("Devolución registrada.\n\nDaños: " + UtilidadesInterfaz.soles(totalDaniosErht) + "\n" + liquidacionErht);
        }));

        HBox filaErht = new HBox(10, UtilidadesInterfaz.etiqueta("Fecha de devolución"), fechaErht,
                UtilidadesInterfaz.etiqueta("Método para liquidar la garantía"), metodoErht);
        filaErht.setAlignment(Pos.CENTER_LEFT);
        HBox botonErht = new HBox(registrarErht);
        botonErht.setAlignment(Pos.CENTER_RIGHT);
        return UtilidadesInterfaz.panel(UtilidadesInterfaz.seccion("Registrar devolución"),
                UtilidadesInterfaz.subtitulo("Marca solo las piezas dañadas. Si no escribes el monto se calcula solo: "
                        + "LEVE 10%, GRAVE 35% y PERDIDA 100% del valor de reposición. "
                        + "Los daños se descuentan de la garantía " + UtilidadesInterfaz.soles(a.montoGarantia()) + "."),
                filaErht, rejillaErht, botonErht);
    }

    private void hacerErht(UtilidadesInterfaz.Accion accion, String exito) {
        if (UtilidadesInterfaz.ejecutar(() -> {
            accion.ejecutar();
            cargarErht();
        })) {
            UtilidadesInterfaz.mostrarInformacion(exito);
        }
    }

    private TableView<AlquilerDetalleTraje> tablaTrajesErht(List<AlquilerDetalleTraje> trajes) {
        TableView<AlquilerDetalleTraje> t = UtilidadesInterfaz.tabla("Sin trajes.");
        t.getColumns().add(UtilidadesInterfaz.colTexto("Código", AlquilerDetalleTraje::codigo, 100));
        t.getColumns().add(UtilidadesInterfaz.colTexto("Danza", AlquilerDetalleTraje::danza, 170));
        t.getColumns().add(UtilidadesInterfaz.colTexto("Personaje", AlquilerDetalleTraje::personaje, 200));
        t.getColumns().add(UtilidadesInterfaz.colTexto("Talla", AlquilerDetalleTraje::talla, 60));
        t.getColumns().add(UtilidadesInterfaz.colSoles("Precio", AlquilerDetalleTraje::precio, 110));
        t.getColumns().add(UtilidadesInterfaz.colSoles("Garantía", AlquilerDetalleTraje::garantia, 110));
        t.getItems().setAll(trajes);
        t.setPrefHeight(Math.min(60 + 30 * trajes.size(), 260));
        return t;
    }

    private TableView<PagoFila> tablaPagosErht(List<PagoFila> pagos) {
        TableView<PagoFila> t = UtilidadesInterfaz.tabla("Sin pagos registrados.");
        t.getColumns().add(UtilidadesInterfaz.colFecha("Fecha", PagoFila::fechaPago, 95));
        t.getColumns().add(UtilidadesInterfaz.colTexto("Concepto", PagoFila::concepto, 170));
        t.getColumns().add(UtilidadesInterfaz.colTexto("Método", PagoFila::metodo, 110));
        t.getColumns().add(UtilidadesInterfaz.colSoles("Monto", PagoFila::monto, 110));
        t.getItems().setAll(pagos);
        t.setPrefHeight(200);
        return t;
    }

    private TableView<DanioFila> tablaDaniosErht(List<DanioFila> danios) {
        TableView<DanioFila> t = UtilidadesInterfaz.tabla("Sin daños registrados.");
        t.getColumns().add(UtilidadesInterfaz.colTexto("Traje", DanioFila::codigo, 80));
        t.getColumns().add(UtilidadesInterfaz.colTexto("Pieza", DanioFila::pieza, 110));
        t.getColumns().add(UtilidadesInterfaz.colEstado("Tipo", DanioFila::tipo, 90));
        t.getColumns().add(UtilidadesInterfaz.colTexto("Descripción", DanioFila::descripcion, 140));
        t.getColumns().add(UtilidadesInterfaz.colSoles("Cobro", DanioFila::montoCobro, 100));
        t.getItems().setAll(danios);
        t.setPrefHeight(200);
        return t;
    }

    private static VBox columnaErht(String titulo, TableView<?> tabla) {
        VBox cajaErht = new VBox(8, UtilidadesInterfaz.seccion(titulo), tabla);
        HBox.setHgrow(cajaErht, Priority.ALWAYS);
        cajaErht.setPrefWidth(500);
        return cajaErht;
    }
}
