package com.trajes;

import com.trajes.fx.AppFx;
import javafx.application.Application;


/**
 * Punto de entrada: ejecuta esta clase.
 * No extiende Application para evitar el error "JavaFX runtime components are missing".
 */
public class Launcher {

    public static void main(String[] args) {
        Application.launch(AppFx.class, args);
    }
}
