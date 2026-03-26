package ca.udem.ift2015.autocompleter;

import ca.udem.ift2015.autocompleter.gui.AppGUI;
import javafx.application.Application;

/**
 * Point d'entrée du GUI JavaFX.
 *
 * <h2>Lancement</h2>
 * <pre>
 * mvn javafx:run
 * </pre>
 */
public class Main {
    public static void main(String[] args) {
        Application.launch(AppGUI.class, args);
    }
}
