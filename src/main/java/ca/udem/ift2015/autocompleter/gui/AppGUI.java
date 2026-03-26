package ca.udem.ift2015.autocompleter.gui;

import ca.udem.ift2015.autocompleter.model.NGramModel;
import ca.udem.ift2015.autocompleter.student.HeapTopKStrategy;
import ca.udem.ift2015.autocompleter.student.KatzBackoffModel;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.File;
import java.util.Arrays;

/**
 * GUI JavaFX — navigation par pages.
 *
 * <p>Pages : Accueil · Corpus · Démo · Benchmark · Autograder
 */
public class AppGUI extends Application {

    // ── Shared state ──────────────────────────────────────────────────────────
    /** Single-element array used as a mutable model reference shared with pages. */
    final NGramModel[] modelRef = { new KatzBackoffModel(new HeapTopKStrategy()) };
    final ObservableList<CorpusFile> corpusFiles = FXCollections.observableArrayList();

    // ── Root layout ───────────────────────────────────────────────────────────
    private BorderPane root;
    private Label      statusLabel;

    // ── Pages ─────────────────────────────────────────────────────────────────
    private Pane          mainPage;
    private CorpusPage    corpusPage;
    private DemoPage      demoPage;
    private BenchmarkPage benchmarkPage;
    private AutograderPage autograderPage;

    // =========================================================================
    // Application entry
    // =========================================================================

    @Override
    public void start(Stage stage) {
        loadCorpusFiles();

        corpusPage     = new CorpusPage(modelRef, corpusFiles, this::onModelChanged, () -> nav(mainPage));
        demoPage       = new DemoPage(modelRef, () -> nav(mainPage));
        benchmarkPage  = new BenchmarkPage(stage, () -> nav(mainPage));
        autograderPage = new AutograderPage(() -> nav(mainPage));
        mainPage       = buildMainPage();

        statusLabel = Styles.lbl("  Modèle : non entraîné", Styles.MUTED, 12, false);
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(6, 16, 6, 16));
        statusBar.setStyle(
                "-fx-background-color:" + Styles.SURFACE + ";" +
                "-fx-border-color:" + Styles.BORDER + ";-fx-border-width:1 0 0 0;");

        root = new BorderPane();
        root.setStyle("-fx-background-color:" + Styles.BG + ";");
        root.setCenter(mainPage);
        root.setBottom(statusBar);

        stage.setTitle("Autocomplétion IFT2015");
        stage.setScene(new Scene(root, 1020, 700));
        stage.setMinWidth(820);
        stage.setMinHeight(560);
        stage.show();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void nav(Pane page) { root.setCenter(page); }

    // ── Model change callback ─────────────────────────────────────────────────

    private void onModelChanged() {
        refreshStatus();
        demoPage.refresh();
    }

    // ── Corpus file scanning ──────────────────────────────────────────────────

    private void loadCorpusFiles() {
        corpusFiles.clear();
        File dir = new File("src/main/resources/corpus");
        File[] txts = dir.listFiles(f -> f.isFile() && f.getName().endsWith(".txt"));
        if (txts != null) {
            Arrays.sort(txts);
            for (File f : txts) corpusFiles.add(new CorpusFile(f));
        }
    }

    // ── Status bar ────────────────────────────────────────────────────────────

    private void refreshStatus() {
        Platform.runLater(() -> {
            try {
                NGramModel m = modelRef[0];
                if (m.unigramCount() == 0) {
                    statusLabel.setText("  Modèle : non entraîné");
                    statusLabel.setStyle(Styles.ss(Styles.MUTED, 12, false));
                } else {
                    long n = corpusFiles.stream().filter(CorpusFile::isTrained).count();
                    statusLabel.setText(String.format(
                            "  Modèle : %,d unigrammes · %,d bigrammes · %,d trigrammes  " +
                            "(%d fichier%s entraîné%s)",
                            m.unigramCount(), m.bigramCount(), m.trigramCount(),
                            n, n > 1 ? "s" : "", n > 1 ? "s" : ""));
                    statusLabel.setStyle(Styles.ss(Styles.SUCCESS, 12, false));
                }
            } catch (UnsupportedOperationException e) {
                statusLabel.setText("  Modèle : non entraîné");
                statusLabel.setStyle(Styles.ss(Styles.MUTED, 12, false));
            }
        });
    }

    // =========================================================================
    // Page — Accueil
    // =========================================================================

    private Pane buildMainPage() {
        Label title    = Styles.lbl("Autocomplétion IFT2015", Styles.TEXT, 32, true);
        Label subtitle = Styles.lbl("Modèle de langage N-grammes avec repli de Katz — IFT2015",
                                    Styles.MUTED, 14, false);
        VBox titleBox = Styles.vbox(6, Pos.CENTER, title, subtitle);

        Label intro = new Label(
                "Bienvenue dans l'interface du TP Autocomplétion.\n" +
                "Sélectionnez et entraînez vos fichiers corpus, puis explorez les suggestions\n" +
                "ou mesurez les performances de votre implémentation.\n" +
                "Commencez par l'onglet Corpus.");
        intro.setWrapText(true);
        intro.setMaxWidth(520);
        intro.setTextAlignment(TextAlignment.CENTER);
        intro.setStyle(Styles.ss(Styles.MUTED, 13, false) + "-fx-line-spacing:3;");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setAlignment(Pos.CENTER);
        grid.add(navCard(FontAwesomeSolid.FOLDER_OPEN,    "Corpus",     "Sélectionnez et entraînez\nvos fichiers texte",       () -> nav(corpusPage)),     0, 0);
        grid.add(navCard(FontAwesomeSolid.CHART_BAR,      "Benchmark",  "Mesurez les performances\nde vos implémentations",    () -> nav(benchmarkPage)),  1, 0);
        grid.add(navCard(FontAwesomeSolid.COMMENTS,       "Démo",       "Testez l'autocomplétion\nen temps réel",              () -> nav(demoPage)),       0, 1);
        grid.add(navCard(FontAwesomeSolid.GRADUATION_CAP, "Autograder", "Correction automatisée\n(disponible ultérieurement)", () -> nav(autograderPage)), 1, 1);

        VBox center = Styles.vbox(36, Pos.CENTER, titleBox, intro, grid);
        center.setPadding(new Insets(60, 40, 40, 40));
        center.setStyle("-fx-background-color:" + Styles.BG + ";");
        return center;
    }

    private Pane navCard(FontAwesomeSolid icon, String title, String desc, Runnable action) {
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(28);
        fontIcon.setIconColor(Color.web(Styles.PRIMARY));

        Label titleL = Styles.lbl(title, Styles.TEXT,  15, true);
        Label descL  = Styles.lbl(desc,  Styles.MUTED, 12, false);
        descL.setWrapText(true);
        descL.setMaxWidth(160);

        VBox card = new VBox(10, fontIcon, titleL, descL);
        card.setPadding(new Insets(22, 26, 22, 26));
        card.setPrefSize(220, 148);
        card.setAlignment(Pos.TOP_LEFT);
        applyCardStyle(card, false);
        card.setCursor(Cursor.HAND);
        card.setOnMouseEntered(e -> applyCardStyle(card, true));
        card.setOnMouseExited (e -> applyCardStyle(card, false));
        card.setOnMouseClicked(e -> action.run());
        return card;
    }

    private static void applyCardStyle(VBox c, boolean hover) {
        c.setStyle(
                "-fx-background-color:" + (hover ? Styles.P_LIGHT : Styles.SURFACE) + ";" +
                "-fx-background-radius:12;" +
                "-fx-border-color:"     + (hover ? Styles.PRIMARY : Styles.BORDER) + ";" +
                "-fx-border-radius:12;-fx-border-width:1.5;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0," +
                (hover ? "0.13" : "0.06") + ")," + (hover ? "16" : "8") + ",0,0," +
                (hover ? "4" : "2") + ");");
    }
}
