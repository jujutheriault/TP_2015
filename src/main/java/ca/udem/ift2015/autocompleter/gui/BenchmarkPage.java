package ca.udem.ift2015.autocompleter.gui;

import ca.udem.ift2015.autocompleter.model.NGramModel;
import ca.udem.ift2015.autocompleter.student.HeapTopKStrategy;
import ca.udem.ift2015.autocompleter.student.KatzBackoffModel;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.*;

import java.io.*;
import java.util.Arrays;

/**
 * Page Benchmark : mesure de performance sur un ou tous les fichiers corpus.
 */
class BenchmarkPage extends BorderPane {

    private final Stage ownerStage;

    BenchmarkPage(Stage ownerStage, Runnable navBack) {
        this.ownerStage = ownerStage;
        build(navBack);
    }

    private void build(Runnable navBack) {
        // ── Results table ──────────────────────────────────────────────────────
        ObservableList<String[]> rows = FXCollections.observableArrayList();
        TableView<String[]> table = buildBenchTable(rows);

        Label hint = Styles.lbl("Lancez un benchmark pour voir les résultats.", Styles.MUTED, 14, false);
        hint.setTextAlignment(TextAlignment.CENTER);

        StackPane center = new StackPane(hint, table);
        table.setVisible(false);

        // ── Status / progress ──────────────────────────────────────────────────
        Label  statusLbl = Styles.lbl("", Styles.MUTED, 12, false);
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(18, 18);
        spinner.setVisible(false);
        HBox progressBox = new HBox(8, spinner, statusLbl);
        progressBox.setAlignment(Pos.CENTER_LEFT);

        // ── File picker ────────────────────────────────────────────────────────
        TextField fileField = new TextField();
        fileField.setEditable(false);
        fileField.setStyle("-fx-font-family:monospace;-fx-font-size:12;");
        HBox.setHgrow(fileField, Priority.ALWAYS);

        Button browseBtn = Styles.ghostBtn("Parcourir…");
        Button runOneBtn = Styles.primaryBtn("▶  Benchmark fichier");
        Button runAllBtn = Styles.ghostBtn("▶  Tous les corpus");
        runOneBtn.setDisable(true);

        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File init = new File("src/main/resources/corpus");
            if (init.exists()) fc.setInitialDirectory(init);
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Texte (*.txt)", "*.txt"));
            File f = fc.showOpenDialog(ownerStage);
            if (f != null) {
                fileField.setText(f.getAbsolutePath());
                runOneBtn.setDisable(false);
            }
        });

        runOneBtn.setOnAction(e -> {
            String path = fileField.getText().trim();
            if (path.isEmpty()) return;
            setRunning(true, runOneBtn, runAllBtn, spinner, statusLbl, "Benchmark en cours…");
            rows.clear(); table.setVisible(false); hint.setVisible(false);
            new Thread(() -> {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (PrintStream ps = new PrintStream(baos, true, "UTF-8")) {
                        NGramModel m = new KatzBackoffModel(new HeapTopKStrategy());
                        Benchmark.runSuite(new NGramModel[]{m}, new String[]{path}, ps);
                    }
                    showResults(baos.toString("UTF-8"), rows, table, hint);
                } catch (Exception ex) {
                    Platform.runLater(() -> statusLbl.setText("Erreur : " + ex.getMessage()));
                } finally {
                    setRunning(false, runOneBtn, runAllBtn, spinner, statusLbl, "");
                }
            }).start();
        });

        runAllBtn.setOnAction(e -> {
            File dir = new File("src/main/resources/corpus");
            File[] files = dir.listFiles(f -> f.getName().endsWith(".txt"));
            if (files == null || files.length == 0) {
                statusLbl.setText("Aucun fichier .txt trouvé dans le corpus.");
                return;
            }
            Arrays.sort(files);
            String[]    paths = Arrays.stream(files).map(File::getAbsolutePath).toArray(String[]::new);
            NGramModel[] ms   = new NGramModel[paths.length];
            for (int i = 0; i < ms.length; i++) ms[i] = new KatzBackoffModel(new HeapTopKStrategy());

            setRunning(true, runOneBtn, runAllBtn, spinner, statusLbl,
                    "Suite en cours  (" + paths.length + " fichiers)…");
            rows.clear(); table.setVisible(false); hint.setVisible(false);
            new Thread(() -> {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (PrintStream ps = new PrintStream(baos, true, "UTF-8")) {
                        Benchmark.runSuite(ms, paths, ps);
                    }
                    showResults(baos.toString("UTF-8"), rows, table, hint);
                } catch (Exception ex) {
                    Platform.runLater(() -> statusLbl.setText("Erreur : " + ex.getMessage()));
                } finally {
                    setRunning(false, runOneBtn, runAllBtn, spinner, statusLbl, "");
                }
            }).start();
        });

        // ── Toolbar ────────────────────────────────────────────────────────────
        Region sep = new Region();
        sep.setPrefSize(1, 28);
        sep.setStyle("-fx-background-color:" + Styles.BORDER + ";");

        HBox fileRow = new HBox(8, new Label("Corpus :"), fileField, browseBtn, runOneBtn);
        fileRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(fileField, Priority.ALWAYS);
        HBox.setHgrow(fileRow,   Priority.ALWAYS);

        HBox toolbar = new HBox(14, fileRow, sep, runAllBtn, progressBox);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(12, 16, 12, 16));
        toolbar.setStyle(
                "-fx-background-color:" + Styles.SURFACE + ";" +
                "-fx-border-color:" + Styles.BORDER + ";-fx-border-width:0 0 1 0;");

        setTop(new VBox(Styles.pageHeader("Benchmark", navBack), toolbar));
        setCenter(center);
        setStyle("-fx-background-color:" + Styles.BG + ";");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void setRunning(boolean running,
                                   Button b1, Button b2,
                                   ProgressIndicator spinner, Label status, String msg) {
        Platform.runLater(() -> {
            b1.setDisable(running || b1.getUserData() == Boolean.FALSE);
            b2.setDisable(running);
            spinner.setVisible(running);
            status.setText(running ? msg : "");
        });
    }

    private static void showResults(String csv, ObservableList<String[]> rows,
                                    TableView<String[]> table, Label hint) {
        Platform.runLater(() -> {
            rows.clear();
            for (String line : csv.trim().split("\n")) {
                if (line.isBlank() || line.startsWith("corpus")) continue;
                String[] c = line.split(",");
                if (c.length < 8) continue;
                rows.add(new String[]{
                        c[0].trim(),
                        Styles.fmtNum(c[1]) + " ms",
                        Styles.fmtNum(c[2]),
                        Styles.fmtNum(c[3]),
                        Styles.fmtNum(c[4]),
                        c[5].trim() + " MB",
                        Styles.fmtNum(c[6]) + " µs",
                        Styles.fmtNum(c[7]) + " µs",
                });
            }
            boolean hasData = !rows.isEmpty();
            table.setVisible(hasData);
            hint.setVisible(!hasData);
        });
    }

    private static TableView<String[]> buildBenchTable(ObservableList<String[]> rows) {
        TableView<String[]> table = new TableView<>(rows);
        table.setStyle("-fx-font-size:13;");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        record ColDef(String header, int idx, int pref, boolean rightAlign) {}
        var defs = java.util.List.of(
                new ColDef("Corpus",         0,   0, false),
                new ColDef("Entraîn.",       1,  95, true),
                new ColDef("Unigrammes",     2, 115, true),
                new ColDef("Bigrammes",      3, 105, true),
                new ColDef("Trigrammes",     4, 105, true),
                new ColDef("Mémoire",        5,  95, true),
                new ColDef("topK moy.",      6,  90, true),
                new ColDef("complete moy.",  7, 110, true)
        );

        TableColumn<String[], String> nameCol = null;
        int fixedTotal = defs.stream().skip(1).mapToInt(ColDef::pref).sum();

        for (ColDef d : defs) {
            TableColumn<String[], String> col = new TableColumn<>(d.header());
            col.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue()[d.idx()]));
            if (d.pref() > 0) { col.setPrefWidth(d.pref()); col.setMinWidth(d.pref() - 10); }
            if (d.rightAlign()) {
                col.setCellFactory(tc -> new TableCell<>() {
                    @Override protected void updateItem(String v, boolean empty) {
                        super.updateItem(v, empty);
                        setText(empty || v == null ? null : v);
                        setStyle("-fx-alignment:CENTER-RIGHT;-fx-font-weight:bold;");
                    }
                });
            }
            if (d.idx() == 0) nameCol = col;
            table.getColumns().add(col);
        }

        if (nameCol != null) {
            final TableColumn<String[], String> nc = nameCol;
            nc.prefWidthProperty().bind(table.widthProperty().subtract(fixedTotal + 18));
        }
        return table;
    }
}
