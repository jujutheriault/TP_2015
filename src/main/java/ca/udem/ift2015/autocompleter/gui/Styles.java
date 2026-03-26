package ca.udem.ift2015.autocompleter.gui;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Constantes de palette et helpers de mise en forme partagés entre toutes les pages.
 */
class Styles {

    // ── Palette ───────────────────────────────────────────────────────────────
    static final String BG      = "#f5f6fa";
    static final String SURFACE = "#ffffff";
    static final String BORDER  = "#e2e8f0";
    static final String PRIMARY = "#4f46e5";
    static final String P_LIGHT = "#ede9fe";
    static final String TEXT    = "#1e293b";
    static final String MUTED   = "#64748b";
    static final String SUCCESS = "#16a34a";
    static final String DANGER  = "#dc2626";

    // ── Labels ────────────────────────────────────────────────────────────────

    static Label lbl(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setStyle(ss(color, size, bold));
        return l;
    }

    static String ss(String color, int size, boolean bold) {
        return "-fx-text-fill:" + color + ";-fx-font-size:" + size + ";" +
               (bold ? "-fx-font-weight:bold;" : "");
    }

    static VBox vbox(int spacing, Pos align, javafx.scene.Node... nodes) {
        VBox b = new VBox(spacing, nodes);
        b.setAlignment(align);
        return b;
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    static Button primaryBtn(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color:" + PRIMARY + ";-fx-text-fill:white;" +
                "-fx-font-weight:bold;-fx-background-radius:8;" +
                "-fx-padding:8 18 8 18;-fx-cursor:hand;-fx-font-size:13;");
        return b;
    }

    static Button ghostBtn(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + MUTED + ";" +
                "-fx-border-color:" + BORDER + ";-fx-border-radius:8;" +
                "-fx-background-radius:8;-fx-padding:7 14 7 14;" +
                "-fx-cursor:hand;-fx-font-size:12;");
        return b;
    }

    // ── Page header ───────────────────────────────────────────────────────────

    static HBox pageHeader(String title, Runnable navBack) {
        Button back = new Button("← Retour");
        back.setStyle(
                "-fx-background-color:transparent;" +
                "-fx-text-fill:" + PRIMARY + ";-fx-font-size:13;" +
                "-fx-cursor:hand;-fx-border-color:transparent;-fx-padding:4 10 4 0;");
        back.setOnMouseEntered(e -> back.setStyle(back.getStyle() + "-fx-underline:true;"));
        back.setOnMouseExited (e -> back.setStyle(back.getStyle().replace("-fx-underline:true;", "")));
        back.setOnAction(e -> navBack.run());

        Region sep = new Region();
        sep.setPrefSize(1, 20);
        sep.setStyle("-fx-background-color:" + BORDER + ";");

        Label titL = lbl(title, TEXT, 18, true);

        HBox header = new HBox(14, back, sep, titL);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle(
                "-fx-background-color:" + SURFACE + ";" +
                "-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");
        return header;
    }

    // ── Number formatting ─────────────────────────────────────────────────────

    static String fmtNum(String raw) {
        try { return String.format("%,d", Long.parseLong(raw.trim())); }
        catch (NumberFormatException e) { return raw.trim(); }
    }

    static String fmtInt(int n) { return String.format("%,d", n); }
}
