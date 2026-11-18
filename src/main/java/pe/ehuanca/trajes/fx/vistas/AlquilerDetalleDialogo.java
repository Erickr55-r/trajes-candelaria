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

    private final Servicios s;
    private final int idAlquiler;
    private final BorderPane raiz = new BorderPane();
    private Stage ventanaDialogo;

    public AlquilerDetalleDialogo(Servicios s, int idAlquiler) {
        this.s = s;
        this.idAlquiler = idAlquiler;
    }

    /** Abre el diálogo y espera a que se cierre. */
    public void mostrar() {
        if (!UtilidadesInterfaz.ejecutar(this::cargar)) {
            return;
        }
        ventanaDialogo = UtilidadesInterfaz.ventana("Alquiler N° " + idAlquiler, raiz, 1080, 760);
        ventanaDialogo.showAndWait();
    }

    private void cargar() {
        AlquilerResumen a = s.alquileres().buscar(idAlquiler);
        List<AlquilerDetalleTraje> trajes = s.alquileres().trajes(idAlquiler);
        List<PagoFila> pagos = s.alquileres().pagos(idAlquiler);
        List<DanioFila> danios = s.alquileres().danios(idAlquiler);

        raiz.setTop(cabecera(a));

        HBox tarjetas = new HBox(12,
                UtilidadesInterfaz.tarjeta("Monto del alquiler", UtilidadesInterfaz.soles(a.montoAlquiler()), "granate"),
                UtilidadesInterfaz.tarjeta("Pagado del alquiler", UtilidadesInterfaz.soles(a.pagadoAlquiler()), "verde"),
                UtilidadesInterfaz.tarjeta("Saldo pendiente", UtilidadesInterfaz.soles(a.saldo()), a.saldo().signum() > 0 ? "naranja" : "verde"),
                UtilidadesInterfaz.tarjeta("Garantía", UtilidadesInterfaz.soles(a.montoGarantia()), "dorado"),
                UtilidadesInterfaz.tarjeta("Daños", UtilidadesInterfaz.soles(a.montoDanios()), a.montoDanios().signum() > 0 ? "rojo" : "gris"));

        VBox cuerpo = new VBox(16, tarjetas);
        cuerpo.getStyleClass().add("contenido");
        switch (a.estado()) {
            case "SOLICITADO" -> cuerpo.getChildren().add(accionesSolicitud(a));
            case "RECHAZADO" -> cuerpo.getChildren().add(UtilidadesInterfaz.panel(UtilidadesInterfaz.seccion("Solicitud rechazada"),
                    UtilidadesInterfaz.subtitulo("Motivo: " + UtilidadesInterfaz.texto(s.alquileres().motivoRechazo(idAlquiler)))));
            case "RESERVADO" -> cuerpo.getChildren().add(accionesReserva(a));
            case "ENTREGADO" -> cuerpo.getChildren().add(devolucion(a));
            default -> { }
        }
        cuerpo.getChildren().addAll(
                UtilidadesInterfaz.seccion("Trajes (" + trajes.size() + ")"), tablaTrajes(trajes),
                new HBox(16, columna("Pagos", tablaPagos(pagos)), columna("Daños registrados", tablaDanios(danios))));

        ScrollPane desplazable = new ScrollPane(cuerpo);
        desplazable.setFitToWidth(true);
        raiz.setCenter(desplazable);

        Button cerrar = new Button("Cerrar");
        cerrar.setOnAction(e -> ventanaDialogo.close());
        HBox pie = new HBox(cerrar);
        pie.getStyleClass().add("barra-acciones");
        raiz.setBottom(pie);
    }

    private VBox cabecera(AlquilerResumen a) {
        HBox linea = new HBox(12, UtilidadesInterfaz.titulo("Alquiler N° " + a.idAlquiler()), UtilidadesInterfaz.insigniasAlquiler(a));
        linea.setAlignment(Pos.CENTER_LEFT);
        VBox caja = new VBox(4, linea,
                UtilidadesInterfaz.subtitulo("Cliente: " + a.cliente()
                        + (a.conjunto() != null ? "   ·   Conjunto: " + a.conjunto() : "")),
                UtilidadesInterfaz.subtitulo("Salida: " + UtilidadesInterfaz.fecha(a.fechaSalida())
                        + "   ·   Devolución prevista: " + UtilidadesInterfaz.fecha(a.fechaDevolucionPrevista())
                        + (a.fechaDevolucionReal() != null ? "   ·   Devuelto: " + UtilidadesInterfaz.fecha(a.fechaDevolucionReal()) : "")
                        + "   ·   " + a.numTrajes() + " traje(s)"));
        if (a.observacion() != null && !a.observacion().isBlank()) {
            caja.getChildren().add(UtilidadesInterfaz.subtitulo("Observación: " + a.observacion()));
        }
        caja.getStyleClass().add("cabecera");
        return caja;
    }

    // ------------------------------------------------------------------ acciones

    /** Solicitud hecha por el cliente desde su portal: la tienda la aprueba o la rechaza con un motivo. */
    private VBox accionesSolicitud(AlquilerResumen a) {
        Button aprobar = UtilidadesInterfaz.estilo(new Button("Aprobar solicitud"), "boton-primario");
        aprobar.setOnAction(e -> {
            if (UtilidadesInterfaz.confirmar("La solicitud N° " + idAlquiler + " de " + a.cliente() + " pasará a RESERVADO.\n"
                    + "Después podrás registrar el adelanto y la entrega. ¿Aprobar?")) {
                hacer(() -> s.alquileres().aprobar(idAlquiler), "Solicitud aprobada: ahora es una reserva.");
            }
        });
        TextField motivo = UtilidadesInterfaz.campo("", "Motivo del rechazo (lo verá el cliente)");
        HBox.setHgrow(motivo, Priority.ALWAYS);
        Button rechazar = UtilidadesInterfaz.estilo(new Button("Rechazar"), "boton-peligro");
        rechazar.setOnAction(e -> {
            if (UtilidadesInterfaz.confirmar("¿Rechazar la solicitud N° " + idAlquiler + "? Los trajes quedarán libres.")) {
                hacer(() -> s.alquileres().rechazar(idAlquiler, motivo.getText()), "Solicitud rechazada.");
            }
        });
        HBox fila1 = new HBox(10, aprobar);
        fila1.setAlignment(Pos.CENTER_LEFT);
        HBox fila2 = new HBox(10, UtilidadesInterfaz.etiqueta("Motivo"), motivo, rechazar);
        fila2.setAlignment(Pos.CENTER_LEFT);
        return UtilidadesInterfaz.panel(UtilidadesInterfaz.seccion("Solicitud del cliente por aprobar"),
                UtilidadesInterfaz.subtitulo("El cliente pidió estos trajes desde su portal. Mientras esté SOLICITADO, nadie más puede "
                        + "pedirlos en esas fechas."),
                fila1, fila2);
    }

    private VBox accionesReserva(AlquilerResumen a) {
        ComboBox<String> metodo = UtilidadesInterfaz.comboTexto(UtilidadesInterfaz.METODOS, null);
        metodo.setPrefWidth(170);

        Button entregar = UtilidadesInterfaz.estilo(new Button("Registrar entrega"), "boton-primario");
        entregar.setOnAction(e -> {
            String m = metodo.getValue();
            if (UtilidadesInterfaz.confirmar("Se entregarán los trajes y se cobrará con " + m + ":\n\n"
                    + "  · Saldo del alquiler: " + UtilidadesInterfaz.soles(a.saldo()) + "\n"
                    + "  · Garantía: " + UtilidadesInterfaz.soles(a.montoGarantia()) + "\n\n¿Registrar la entrega?")) {
                hacer(() -> s.alquileres().entregar(idAlquiler, m), "Entrega registrada.");
            }
        });

        TextField adelanto = UtilidadesInterfaz.campo("", "Monto S/");
        adelanto.setPrefWidth(120);
        Button pagar = UtilidadesInterfaz.estilo(new Button("Registrar adelanto"), "boton-dorado");
        pagar.setOnAction(e -> hacer(() -> s.alquileres().registrarPago(idAlquiler, "ADELANTO",
                UtilidadesInterfaz.monto(adelanto.getText()), metodo.getValue()), "Adelanto registrado."));

        Button anular = UtilidadesInterfaz.estilo(new Button("Anular alquiler"), "boton-peligro");
        anular.setOnAction(e -> {
            if (UtilidadesInterfaz.confirmar("¿Seguro que deseas ANULAR el alquiler N° " + idAlquiler + "?\n"
                    + "Los trajes quedarán libres para esas fechas. Esta acción no se puede deshacer.")) {
                hacer(() -> s.alquileres().anular(idAlquiler), "Alquiler anulado.");
            }
        });

        HBox fila1 = new HBox(10, UtilidadesInterfaz.etiqueta("Método de pago"), metodo, entregar, UtilidadesInterfaz.espacio(), anular);
        fila1.setAlignment(Pos.CENTER_LEFT);
        HBox fila2 = new HBox(10, UtilidadesInterfaz.etiqueta("Adelanto"), adelanto, pagar);
        fila2.setAlignment(Pos.CENTER_LEFT);
        return UtilidadesInterfaz.panel(UtilidadesInterfaz.seccion("Acciones de la reserva"),
                UtilidadesInterfaz.subtitulo("Al entregar se cobra el saldo pendiente y la garantía con el método elegido."),
                fila1, fila2);
    }

    private VBox devolucion(AlquilerResumen a) {
        DatePicker fecha = UtilidadesInterfaz.selectorFecha(LocalDate.now());
        fecha.setMaxWidth(160);
        ComboBox<String> metodo = UtilidadesInterfaz.comboTexto(UtilidadesInterfaz.METODOS, null);
        metodo.setPrefWidth(170);

        List<PiezaAlquiler> piezas = s.alquileres().piezasDelAlquiler(idAlquiler);
        GridPane rejilla = new GridPane();
        rejilla.setHgap(10);
        rejilla.setVgap(8);
        rejilla.addRow(0, UtilidadesInterfaz.etiqueta("Pieza dañada"), UtilidadesInterfaz.etiqueta("Tipo"), UtilidadesInterfaz.etiqueta("Descripción"),
                UtilidadesInterfaz.etiqueta("Monto S/ (vacío = automático)"));
        List<ComboBox<PiezaAlquiler>> cPiezas = new ArrayList<>();
        List<ComboBox<String>> cTipos = new ArrayList<>();
        List<TextField> cDescripciones = new ArrayList<>();
        List<TextField> cMontos = new ArrayList<>();
        for (int k = 0; k < FILAS_DANIO; k++) {
            ComboBox<PiezaAlquiler> pieza = UtilidadesInterfaz.combo(piezas,
                    p -> p.etiqueta() + " · repos. " + UtilidadesInterfaz.soles(p.valorReposicion()), "— Sin daño —");
            pieza.setPrefWidth(470);
            ComboBox<String> tipo = UtilidadesInterfaz.comboTexto(List.of("LEVE", "GRAVE", "PERDIDA"), null);
            tipo.setPrefWidth(110);
            TextField descripcion = UtilidadesInterfaz.campo("", "Ej.: costura rota");
            TextField monto = UtilidadesInterfaz.campo("", "Automático");
            monto.setPrefWidth(130);
            rejilla.addRow(k + 1, pieza, tipo, descripcion, monto);
            GridPane.setHgrow(descripcion, Priority.ALWAYS);
            cPiezas.add(pieza);
            cTipos.add(tipo);
            cDescripciones.add(descripcion);
            cMontos.add(monto);
        }

        Button registrar = UtilidadesInterfaz.estilo(new Button("Registrar devolución"), "boton-primario");
        registrar.setOnAction(e -> UtilidadesInterfaz.ejecutar(() -> {
            LocalDate fechaReal = UtilidadesInterfaz.requerido(fecha, "Fecha de devolución");
            String[] idPiezas = new String[FILAS_DANIO];
            String[] tipos = new String[FILAS_DANIO];
            String[] descripciones = new String[FILAS_DANIO];
            String[] montos = new String[FILAS_DANIO];
            int conDanio = 0;
            for (int k = 0; k < FILAS_DANIO; k++) {
                PiezaAlquiler p = cPiezas.get(k).getValue();
                idPiezas[k] = p == null ? "" : String.valueOf(p.idPieza());
                tipos[k] = cTipos.get(k).getValue();
                descripciones[k] = cDescripciones.get(k).getText();
                BigDecimal m = UtilidadesInterfaz.monto(cMontos.get(k).getText());
                montos[k] = m == null ? "" : m.toPlainString();
                if (p != null) {
                    conDanio++;
                }
            }
            if (!UtilidadesInterfaz.confirmar("Se registrará la devolución del " + UtilidadesInterfaz.fecha(fechaReal) + " con "
                    + (conDanio == 0 ? "ningún daño" : conDanio + " pieza(s) dañada(s)")
                    + ".\nLa garantía se liquida con " + metodo.getValue() + ". ¿Continuar?")) {
                return;
            }
            BigDecimal totalDanios = s.alquileres().devolver(idAlquiler, fechaReal, idPiezas, tipos,
                    descripciones, montos, metodo.getValue());
            BigDecimal diferencia = a.montoGarantia().subtract(totalDanios);
            String liquidacion = diferencia.signum() > 0
                    ? "Se devuelve al cliente " + UtilidadesInterfaz.soles(diferencia) + " de la garantía."
                    : diferencia.signum() < 0
                    ? "Los daños superan la garantía: se cobró " + UtilidadesInterfaz.soles(diferencia.negate()) + "."
                    : "La garantía cubre exactamente los daños.";
            cargar();
            UtilidadesInterfaz.mostrarInformacion("Devolución registrada.\n\nDaños: " + UtilidadesInterfaz.soles(totalDanios) + "\n" + liquidacion);
        }));

        HBox fila = new HBox(10, UtilidadesInterfaz.etiqueta("Fecha de devolución"), fecha,
                UtilidadesInterfaz.etiqueta("Método para liquidar la garantía"), metodo);
        fila.setAlignment(Pos.CENTER_LEFT);
        HBox boton = new HBox(registrar);
        boton.setAlignment(Pos.CENTER_RIGHT);
        return UtilidadesInterfaz.panel(UtilidadesInterfaz.seccion("Registrar devolución"),
                UtilidadesInterfaz.subtitulo("Marca solo las piezas dañadas. Si no escribes el monto se calcula solo: "
                        + "LEVE 10%, GRAVE 35% y PERDIDA 100% del valor de reposición. "
                        + "Los daños se descuentan de la garantía " + UtilidadesInterfaz.soles(a.montoGarantia()) + "."),
                fila, rejilla, boton);
    }

    /** Ejecuta una acción, recarga el diálogo y muestra el mensaje de éxito. */
    private void hacer(UtilidadesInterfaz.Accion accion, String exito) {
        if (UtilidadesInterfaz.ejecutar(() -> {
            accion.ejecutar();
            cargar();
        })) {
            UtilidadesInterfaz.mostrarInformacion(exito);
        }
    }

    // ------------------------------------------------------------------ tablas

    private TableView<AlquilerDetalleTraje> tablaTrajes(List<AlquilerDetalleTraje> trajes) {
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

    private TableView<PagoFila> tablaPagos(List<PagoFila> pagos) {
        TableView<PagoFila> t = UtilidadesInterfaz.tabla("Sin pagos registrados.");
        t.getColumns().add(UtilidadesInterfaz.colFecha("Fecha", PagoFila::fechaPago, 95));
        t.getColumns().add(UtilidadesInterfaz.colTexto("Concepto", PagoFila::concepto, 170));
        t.getColumns().add(UtilidadesInterfaz.colTexto("Método", PagoFila::metodo, 110));
        t.getColumns().add(UtilidadesInterfaz.colSoles("Monto", PagoFila::monto, 110));
        t.getItems().setAll(pagos);
        t.setPrefHeight(200);
        return t;
    }

    private TableView<DanioFila> tablaDanios(List<DanioFila> danios) {
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

    private static VBox columna(String titulo, TableView<?> tabla) {
        VBox caja = new VBox(8, UtilidadesInterfaz.seccion(titulo), tabla);
        HBox.setHgrow(caja, Priority.ALWAYS);
        caja.setPrefWidth(500);
        return caja;
    }
}
