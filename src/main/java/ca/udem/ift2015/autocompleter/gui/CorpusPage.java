package ca.udem.ift2015.autocompleter.gui;

import ca.udem.ift2015.autocompleter.model.NGramModel;
import ca.udem.ift2015.autocompleter.preprocessing.TextTokenizer;
import ca.udem.ift2015.autocompleter.student.HeapTopKStrategy;
import ca.udem.ift2015.autocompleter.student.KatzBackoffModel;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;

import java.io.*;
import java.util.List;
import java.util.function.Function;

/**
 * Page Corpus : sélection, entraînement et statistiques des fichiers texte.
 */
class CorpusPage extends BorderPane {

    CorpusPage(NGramModel[] modelRef,
               ObservableList<CorpusFile> files,
               Runnable onModelChanged,
               Runnable navBack) {

        // ── Table ─────────────────────────────────────────────────────────────
        TableView<CorpusFile> table = new TableView<>(files);
        table.setEditable(true);
        table.setStyle("-fx-font-size:13;");
        table.setPlaceholder(new Label("Aucun fichier .txt dans src/main/resources/corpus/"));

        TableColumn<CorpusFile, String>  nameCol = strCol("Fichier", cf -> cf.file.getName());
        TableColumn<CorpusFile, String>  sizeCol = strCol("Taille",  CorpusFile::getSize);
        TableColumn<CorpusFile, Boolean> selCol  = new TableColumn<>("✓");
        TableColumn<CorpusFile, Number>  uniCol  = cntCol("Unigrammes", cf -> cf.unigramCount);
        TableColumn<CorpusFile, Number>  biCol   = cntCol("Bigrammes",  cf -> cf.bigramCount);
        TableColumn<CorpusFile, Number>  triCol  = cntCol("Trigrammes", cf -> cf.trigramCount);

        sizeCol.setMaxWidth(90);  sizeCol.setMinWidth(65);  sizeCol.setPrefWidth(80);
        selCol .setMaxWidth(55);  selCol .setMinWidth(45);  selCol .setPrefWidth(50);
        uniCol .setPrefWidth(120); uniCol.setMinWidth(90);
        biCol  .setPrefWidth(110); biCol .setMinWidth(85);
        triCol .setPrefWidth(110); triCol.setMinWidth(85);
        nameCol.prefWidthProperty().bind(
                table.widthProperty()
                     .subtract(sizeCol.widthProperty())
                     .subtract(selCol .widthProperty())
                     .subtract(uniCol .widthProperty())
                     .subtract(biCol  .widthProperty())
                     .subtract(triCol .widthProperty())
                     .subtract(18));

        sizeCol.setCellFactory(tc -> rightCell(Styles.MUTED));

        selCol.setCellValueFactory(cd -> cd.getValue().selected);
        selCol.setCellFactory(CheckBoxTableCell.forTableColumn(selCol));
        selCol.setEditable(true);

        //noinspection unchecked
        table.getColumns().setAll(nameCol, sizeCol, selCol, uniCol, biCol, triCol);

        // ── Toolbar ───────────────────────────────────────────────────────────
        Button selAllBtn = Styles.ghostBtn("Tout sélectionner");
        Button deselBtn  = Styles.ghostBtn("Tout désélectionner");
        Button resetBtn  = Styles.ghostBtn("⟳  Réinitialiser le modèle");
        resetBtn.setStyle(resetBtn.getStyle() + "-fx-text-fill:" + Styles.DANGER + ";");

        Button trainBtn = Styles.primaryBtn("Entraîner la sélection");
        Label  msg      = Styles.lbl("", Styles.MUTED, 12, false);

        selAllBtn.setOnAction(e -> files.forEach(cf -> cf.selected.set(true)));
        deselBtn .setOnAction(e -> files.forEach(cf -> cf.selected.set(false)));

        resetBtn.setOnAction(e -> {
            modelRef[0] = new KatzBackoffModel(new HeapTopKStrategy());
            files.forEach(CorpusFile::resetStats);
            table.refresh();
            onModelChanged.run();
            msg.setText("Modèle réinitialisé.");
            msg.setStyle(Styles.ss(Styles.MUTED, 12, false));
        });

        trainBtn.setOnAction(e -> {
            List<CorpusFile> todo = files.stream()
                    .filter(cf -> cf.selected.get() && !cf.isTrained())
                    .toList();
            if (todo.isEmpty()) {
                msg.setText("Aucun nouveau fichier sélectionné à entraîner.");
                msg.setStyle(Styles.ss(Styles.MUTED, 12, false));
                return;
            }
            trainBtn.setDisable(true);
            msg.setText("Entraînement en cours…");
            msg.setStyle(Styles.ss(Styles.MUTED, 12, false));

            new Thread(() -> {
                boolean anyError = false;
                for (CorpusFile cf : todo) {
                    try {
                        List<List<String>> sents =
                                new TextTokenizer(new FileReader(cf.file)).tokenize();
                        NGramModel tmp = new KatzBackoffModel(new HeapTopKStrategy());
                        tmp.train(sents);
                        modelRef[0].train(sents);
                        int u = tmp.unigramCount(), b = tmp.bigramCount(), t = tmp.trigramCount();
                        Platform.runLater(() -> {
                            cf.unigramCount.set(u);
                            cf.bigramCount .set(b);
                            cf.trigramCount.set(t);
                            table.refresh();
                        });
                    } catch (IOException ex) {
                        anyError = true;
                        Platform.runLater(() -> {
                            msg.setText("Erreur lecture : " + cf.file.getName());
                            msg.setStyle(Styles.ss(Styles.DANGER, 12, false));
                        });
                    } catch (UnsupportedOperationException ex) {
                        anyError = true;
                        Platform.runLater(() -> {
                            msg.setText("TODO non implémenté — l'entraînement requiert les TODOs 1-5, 7 et 10.");
                            msg.setStyle(Styles.ss(Styles.DANGER, 12, false));
                        });
                        break; // inutile de continuer avec les autres fichiers
                    }
                }
                final boolean hadError = anyError;
                Platform.runLater(() -> {
                    trainBtn.setDisable(false);
                    if (!hadError) {
                        msg.setText("Entraînement terminé.");
                        msg.setStyle(Styles.ss(Styles.SUCCESS, 12, false));
                    }
                    onModelChanged.run();
                });
            }).start();
        });

        HBox left  = new HBox(8, selAllBtn, deselBtn, resetBtn);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox right = new HBox(10, msg, trainBtn);
        right.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(left,  Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox toolbar = new HBox(left, right);
        toolbar.setPadding(new Insets(12, 16, 12, 16));
        toolbar.setStyle(
                "-fx-background-color:" + Styles.SURFACE + ";" +
                "-fx-border-color:" + Styles.BORDER + ";-fx-border-width:1 0 0 0;");

        setTop(Styles.pageHeader("Corpus", navBack));
        setCenter(table);
        setBottom(toolbar);
        setStyle("-fx-background-color:" + Styles.BG + ";");
    }

    // ── Table column helpers ──────────────────────────────────────────────────

    private static TableColumn<CorpusFile, String> strCol(
            String title, Function<CorpusFile, String> getter) {
        TableColumn<CorpusFile, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd ->
                new javafx.beans.property.ReadOnlyStringWrapper(getter.apply(cd.getValue())));
        col.setEditable(false);
        return col;
    }

    private static TableColumn<CorpusFile, Number> cntCol(
            String title, Function<CorpusFile, IntegerProperty> prop) {
        TableColumn<CorpusFile, Number> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> prop.apply(cd.getValue()));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null || v.intValue() < 0) {
                    setText("–");
                    setStyle("-fx-text-fill:#94a3b8;-fx-alignment:CENTER-RIGHT;");
                } else {
                    setText(String.format("%,d", v.intValue()));
                    setStyle("-fx-text-fill:" + Styles.TEXT + ";-fx-alignment:CENTER-RIGHT;" +
                             "-fx-font-weight:bold;");
                }
            }
        });
        col.setEditable(false);
        return col;
    }

    private static <S> TableCell<S, String> rightCell(String color) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v);
                setStyle("-fx-alignment:CENTER-RIGHT;-fx-text-fill:" + color + ";");
            }
        };
    }
}
