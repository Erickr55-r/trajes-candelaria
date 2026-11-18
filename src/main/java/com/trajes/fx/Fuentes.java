package com.trajes.fx;

import javafx.scene.text.Font;

import java.io.IOException;
import java.io.InputStream;

public final class Fuentes {

    private static final String[] ARCHIVOS = {
            "PlayfairDisplay-Regular.ttf", "PlayfairDisplay-SemiBold.ttf", "PlayfairDisplay-Bold.ttf",
            "PlusJakartaSans-Regular.ttf", "PlusJakartaSans-Medium.ttf", "PlusJakartaSans-SemiBold.ttf",
            "PlusJakartaSans-Bold.ttf"
    };

    private static boolean cargadas;

    private Fuentes() {
    }

    public static synchronized void cargar() {
        if (cargadas) {
            return;
        }
        for (String archivo : ARCHIVOS) {
            try (InputStream in = Fuentes.class.getResourceAsStream("/fonts/" + archivo)) {
                if (in != null) {
                    Font.loadFont(in, 13);
                }
            } catch (IOException ignorada) {
                
            }
        }
        cargadas = true;
    }
}
