package ca.udem.ift2015.autocompleter.gui;

import ca.udem.ift2015.autocompleter.model.NGramModel;

import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Collections;
import java.util.List;

/**
 * Page Démo : saisie interactive avec suggestions Katz + complétion Trie.
 */
class DemoPage extends BorderPane {

    // ── Shared model reference ────────────────────────────────────────────────
    private final NGramModel[] modelRef;
    private NGramModel model() { return modelRef[0]; }

    // ── UI components ─────────────────────────────────────────────────────────
    private TextArea  demoComposed;
    private TextField demoInput;
    private final Button[] suggBtns = new Button[3];
    private Label    demoStatus;

    // Level table: row 0 = headers, rows 1-5 = data; cols 0-3 = tri/bi/uni/trie
    private final Label[][] levelCells = new Label[6][4];

    // ── Constructor ───────────────────────────────────────────────────────────

    DemoPage(NGramModel[] modelRef, Runnable navBack) {
        this.modelRef = modelRef;
        build(navBack);
    }

    /** Called externally (e.g. after training) to refresh suggestions. */
    void refresh() { refreshSuggestions(); }

    // ── Page construction ─────────────────────────────────────────────────────

    private void build(Runnable navBack) {
        demoComposed = new TextArea();
        demoComposed.setEditable(false);
        demoComposed.setWrapText(true);
        demoComposed.setStyle("-fx-font-size:17;-fx-font-family:SansSerif;");
        demoComposed.setPrefRowCount(5);

        VBox composedBox = new VBox(4,
                Styles.lbl("Texte composé :", Styles.MUTED, 12, false), demoComposed);
        VBox.setVgrow(demoComposed, Priority.ALWAYS);

        // ── Suggestion buttons ────────────────────────────────────────────────
        HBox suggBar = new HBox(8);
        suggBar.setAlignment(Pos.CENTER);
        suggBar.setPadding(new Insets(8, 12, 8, 12));
        suggBar.setStyle(
                "-fx-background-color:#f1f3f9;" +
                "-fx-border-color:" + Styles.BORDER + ";-fx-border-width:1 0 1 0;");
        for (int i = 0; i < 3; i++) {
            Button btn = new Button("–");
            btn.setStyle(
                    "-fx-font-size:14;-fx-font-weight:bold;" +
                    "-fx-background-color:" + Styles.SURFACE + ";" +
                    "-fx-border-color:" + Styles.BORDER + ";-fx-border-radius:8;" +
                    "-fx-background-radius:8;-fx-pref-height:44;-fx-cursor:hand;");
            btn.setDisable(true);
            btn.setMaxWidth(Double.MAX_VALUE);
            final int idx = i;
            btn.setOnAction(ev -> applyDemoSuggestion(idx));
            suggBtns[i] = btn;
            HBox.setHgrow(btn, Priority.ALWAYS);
            suggBar.getChildren().add(btn);
        }

        // ── Input row ─────────────────────────────────────────────────────────
        demoInput = new TextField();
        demoInput.setStyle("-fx-font-size:15;");
        demoInput.setPromptText("Tapez un mot…");
        demoInput.setOnAction(e -> commitCurrentWord());
        demoInput.textProperty().addListener((ob, ov, nv) -> refreshSuggestions());

        Button commitBtn = Styles.primaryBtn("Valider →");
        Button bsBtn     = Styles.ghostBtn("⌫");
        Button clrBtn    = Styles.ghostBtn("Tout effacer");
        commitBtn.setOnAction(e -> commitCurrentWord());
        bsBtn    .setOnAction(e -> deleteLastWord());
        clrBtn   .setOnAction(e -> { demoComposed.clear(); demoInput.clear(); refreshSuggestions(); });

        HBox inputRow = new HBox(8, demoInput, commitBtn, bsBtn, clrBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(demoInput, Priority.ALWAYS);

        demoStatus = Styles.lbl(" ", Styles.MUTED, 12, false);
        demoStatus.setStyle(demoStatus.getStyle() + "-fx-font-style:italic;");

        GridPane levelTable = buildLevelTable();

        VBox bottomBox = new VBox(8, suggBar, inputRow, demoStatus, levelTable);
        bottomBox.setPadding(new Insets(10));

        SplitPane split = new SplitPane(composedBox, bottomBox);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.5);

        BorderPane content = new BorderPane(split);
        content.setPadding(new Insets(12));

        setTop(Styles.pageHeader("Démo", navBack));
        setCenter(content);
        refreshSuggestions();
    }

    // ── Input actions ─────────────────────────────────────────────────────────

    private void commitCurrentWord() {
        String word = demoInput.getText().trim();
        if (!word.isEmpty()) {
            demoComposed.appendText(word + " ");
            demoInput.clear();
        }
        refreshSuggestions();
        demoInput.requestFocus();
    }

    private void deleteLastWord() {
        String text = demoComposed.getText().stripTrailing();
        int ls = text.lastIndexOf(' ');
        demoComposed.setText(ls >= 0 ? text.substring(0, ls + 1) : "");
        refreshSuggestions();
    }

    private void applyDemoSuggestion(int idx) {
        if (suggBtns[idx].isDisabled()) return;
        String word = suggBtns[idx].getText();
        if (word.equals("–") || word.isBlank()) return;
        demoComposed.appendText(word + " ");
        demoInput.clear();
        refreshSuggestions();
        demoInput.requestFocus();
    }

    // ── Suggestion refresh ────────────────────────────────────────────────────

    private void refreshSuggestions() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::refreshSuggestions);
            return;
        }
        try {
            if (model().unigramCount() == 0) {
                for (Button b : suggBtns) { b.setText("–"); b.setDisable(true); }
                if (demoStatus != null) demoStatus.setText(
                        "Entraînez le modèle (onglet Corpus) pour voir des suggestions.");
                resetLevelTable();
                return;
            }

            String composed = demoComposed != null ? demoComposed.getText() : "";
            String partial  = demoInput    != null ? demoInput.getText()    : "";
            String[] ctx    = extractContext(composed);

            List<String> suggs;
            String statusText;
            if (!partial.isEmpty()) {
                suggs = model().complete(partial.toLowerCase(), 3);
                String countStr = suggs.size() == 3 ? "> 3" : String.valueOf(suggs.size());
                statusText = String.format("Trie \"%s\" — %s complétion(s)",
                        partial, countStr);
            } else {
                suggs = model().topK(3, ctx);
                statusText = switch (ctx.length) {
                    case 2  -> String.format("Katz trigramme  \"%s %s\"", ctx[0], ctx[1]);
                    case 1  -> String.format("Katz bigramme   \"%s\"",    ctx[0]);
                    default -> "Katz unigramme  (aucun contexte)";
                };
            }

            for (int i = 0; i < 3; i++) {
                if (i < suggs.size()) { suggBtns[i].setText(suggs.get(i)); suggBtns[i].setDisable(false); }
                else                  { suggBtns[i].setText("–");          suggBtns[i].setDisable(true);  }
            }
            if (demoStatus   != null) demoStatus.setText(statusText);
            updateLevelTable(ctx, partial.toLowerCase(), new java.util.HashSet<>(suggs));
            if (demoComposed != null) demoComposed.positionCaret(demoComposed.getLength());

        } catch (UnsupportedOperationException e) {
            // Student TODOs not yet implemented — degrade gracefully
            for (Button b : suggBtns) { b.setText("–"); b.setDisable(true); }
            if (demoStatus != null) demoStatus.setText(
                    "TODO non implémenté — complétez les TODOs pour utiliser la démo.");
            resetLevelTable();
        }
    }

    /** Resets all level-table cells to "–" without calling any model method. */
    private void resetLevelTable() {
        if (levelCells[0][0] == null) return;
        String[] headers = {"trigramme", "bigramme", "unigramme", "trie"};
        for (int c = 0; c < 4; c++) levelCells[0][c].setText(headers[c]);
        for (int r = 1; r <= 5; r++)
            for (int c = 0; c < 4; c++) {
                levelCells[r][c].setText("–");
                levelCells[r][c].setStyle(Styles.ss(Styles.MUTED, 12, false) + cellBorder(r, c));
            }
    }

    private static String[] extractContext(String composed) {
        String t = composed.stripTrailing();
        if (t.isEmpty()) return new String[0];
        String[] w = t.split("\\s+");
        return w.length >= 2
                ? new String[]{ w[w.length - 2], w[w.length - 1] }
                : new String[]{ w[w.length - 1] };
    }

    // ── Level table ───────────────────────────────────────────────────────────

    private GridPane buildLevelTable() {
        GridPane g = new GridPane();
        g.setStyle(
                "-fx-background-color:" + Styles.SURFACE + ";" +
                "-fx-border-color:" + Styles.BORDER + ";-fx-border-width:1;" +
                "-fx-background-radius:6;-fx-border-radius:6;");

        for (int c = 0; c < 4; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25.0);
            cc.setHalignment(HPos.LEFT);
            g.getColumnConstraints().add(cc);
        }

        // Header row
        String[] hdrInit = { "trigramme", "bigramme", "unigramme", "trie" };
        for (int c = 0; c < 4; c++) {
            Label h = Styles.lbl(hdrInit[c], Styles.MUTED, 11, true);
            h.setPadding(new Insets(5, 10, 5, 10));
            h.setMaxWidth(Double.MAX_VALUE);
            h.setStyle(h.getStyle() + "-fx-background-color:#f0f2f8;" + cellBorder(0, c));
            levelCells[0][c] = h;
            g.add(h, c, 0);
        }

        // Data rows 1-5
        for (int r = 1; r <= 5; r++) {
            for (int c = 0; c < 4; c++) {
                Label cell = new Label("–");
                cell.setPadding(new Insets(4, 10, 4, 10));
                cell.setMaxWidth(Double.MAX_VALUE);
                cell.setStyle(Styles.ss(Styles.MUTED, 12, false) + cellBorder(r, c));
                levelCells[r][c] = cell;
                g.add(cell, c, r);
            }
        }
        return g;
    }

    private static String cellBorder(int row, int col) {
        return "-fx-border-color:" + Styles.BORDER + ";" +
               "-fx-border-width:0 " + (col < 3 ? 1 : 0) + " " + (row < 5 ? 1 : 0) + " 0;";
    }

    private void updateLevelTable(String[] ctx, String partial, java.util.Set<String> highlighted) {
        if (levelCells[0][0] == null) return;

        // Column headers
        levelCells[0][0].setText(ctx.length >= 2
                ? "\"" + ctx[0] + " " + ctx[1] + "\""
                : "trigramme");
        levelCells[0][1].setText(ctx.length >= 1
                ? "\"" + ctx[ctx.length - 1] + "\""
                : "bigramme");
        levelCells[0][2].setText("unigramme");
        levelCells[0][3].setText(partial.isEmpty() ? "trie" : "trie \"" + partial + "\"");

        // Top-5 from each entry point
        List<String> triTop  = ctx.length >= 2 ? model().topK(5, ctx)                   : Collections.emptyList();
        List<String> biTop   = ctx.length >= 1 ? model().topK(5, ctx[ctx.length - 1])   : Collections.emptyList();
        List<String> uniTop  = model().topK(5);
        List<String> trieTop = partial.isEmpty() ? Collections.emptyList()
                                                 : model().complete(partial, 5);

        for (int r = 1; r <= 5; r++) {
            int i = r - 1;
            setCellContent(r, 0, i < triTop.size()  ? triTop.get(i)  : null, highlighted);
            setCellContent(r, 1, i < biTop.size()   ? biTop.get(i)   : null, highlighted);
            setCellContent(r, 2, i < uniTop.size()  ? uniTop.get(i)  : null, highlighted);
            setCellContent(r, 3, i < trieTop.size() ? trieTop.get(i) : null, highlighted);
        }
    }

    private void setCellContent(int row, int col, String word, java.util.Set<String> highlighted) {
        Label cell = levelCells[row][col];
        if (word == null) {
            cell.setText("–");
            cell.setStyle(Styles.ss(Styles.MUTED, 12, false) + cellBorder(row, col));
        } else {
            boolean bold = highlighted.contains(word);
            String color = bold ? Styles.PRIMARY : Styles.TEXT;
            cell.setText(word + "  (" + Styles.fmtInt(model().frequency(word)) + ")");
            cell.setStyle(Styles.ss(color, 12, bold) + cellBorder(row, col));
        }
    }
}
