package ca.udem.ift2015.autocompleter.gui;

import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.util.List;

/**
 * Page Autograder : lance {@code mvn test} et affiche les résultats
 * par composante (HashFrequencyTable · HeapTopKStrategy ·
 * KatzBackoffModel · PrefixTrie) avec détail par test.
 */
class AutograderPage extends BorderPane {

    private final Button            runBtn;
    private final ProgressIndicator spinner;
    private final Label             statusLbl;
    private final Label             totalLbl;
    private final ProgressBar       bar;
    private final Label             pctLbl;
    private final VBox              scorePanel;
    private final Accordion         accordion;

    // ── Constructor ───────────────────────────────────────────────────────────

    AutograderPage(Runnable navBack) {

        // ── Toolbar ───────────────────────────────────────────────────────────
        runBtn  = Styles.primaryBtn("▶  Lancer la correction");
        spinner = new ProgressIndicator();
        spinner.setMaxSize(18, 18);
        spinner.setVisible(false);
        statusLbl = Styles.lbl("", Styles.MUTED, 12, false);

        HBox toolbar = new HBox(12, runBtn, spinner, statusLbl);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(12, 0, 8, 0));

        // ── Score panel (hidden until first run) ──────────────────────────────
        totalLbl = Styles.lbl("Score total : — / 50", Styles.TEXT, 16, true);

        bar = new ProgressBar(0);
        bar.setPrefWidth(320);
        bar.setPrefHeight(14);

        pctLbl = Styles.lbl("", Styles.MUTED, 12, false);
        bar.progressProperty().addListener((obs, ov, nv) ->
                pctLbl.setText(String.format("%.1f %%", nv.doubleValue() * 100)));

        HBox barRow = new HBox(10, bar, pctLbl);
        barRow.setAlignment(Pos.CENTER_LEFT);

        Separator sep2 = new Separator();
        sep2.setPadding(new Insets(6, 0, 6, 0));

        scorePanel = new VBox(8, totalLbl, barRow, sep2);
        scorePanel.setVisible(false);
        scorePanel.setPadding(new Insets(8, 0, 0, 0));

        // ── Accordion ─────────────────────────────────────────────────────────
        accordion = new Accordion();

        // ── Center layout ─────────────────────────────────────────────────────
        Separator sep1 = new Separator();

        VBox center = new VBox(0, toolbar, sep1, scorePanel, accordion);
        center.setPadding(new Insets(16, 20, 16, 20));

        ScrollPane scroll = new ScrollPane(center);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background-color:" + Styles.BG + ";" +
                "-fx-background:" + Styles.BG + ";");

        setTop(Styles.pageHeader("Autograder", navBack));
        setCenter(scroll);
        setStyle("-fx-background-color:" + Styles.BG + ";");

        runBtn.setOnAction(e -> launchGrading());
    }

    // ── Grading flow ──────────────────────────────────────────────────────────

    private void launchGrading() {
        runBtn.setDisable(true);
        spinner.setVisible(true);
        statusLbl.setText("Compilation et tests Maven en cours…");
        statusLbl.setStyle(Styles.ss(Styles.MUTED, 12, false));
        accordion.getPanes().clear();
        scorePanel.setVisible(false);

        new Thread(() -> {
            try {
                File root = findProjectRoot();
                List<Grader.ComponentScore> scores = Grader.grade(root,
                        line -> Platform.runLater(() -> statusLbl.setText(abbreviate(line, 90))));
                Platform.runLater(() -> showResults(scores));
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    runBtn.setDisable(false);
                    spinner.setVisible(false);
                });
            }
        }).start();
    }

    // ── Result display ────────────────────────────────────────────────────────

    private void showResults(List<Grader.ComponentScore> scores) {
        double earned = scores.stream().mapToDouble(Grader.ComponentScore::earned).sum();
        double max    = scores.stream().mapToDouble(Grader.ComponentScore::maxPts).sum();

        totalLbl.setText(String.format("Score total : %.1f / %.0f pts", earned, max));
        bar.setProgress(max > 0 ? earned / max : 0);
        scorePanel.setVisible(true);

        accordion.getPanes().clear();
        for (Grader.ComponentScore cs : scores) {
            accordion.getPanes().add(buildPane(cs));
        }

        boolean allPassed = scores.stream().allMatch(
                cs -> cs.total() > 0 && cs.passed() == cs.total());
        statusLbl.setText(allPassed ? "Tous les tests ont réussi !" : "Correction terminée.");
        statusLbl.setStyle(Styles.ss(allPassed ? Styles.SUCCESS : Styles.MUTED, 12, false));
    }

    private static TitledPane buildPane(Grader.ComponentScore cs) {
        boolean allPass = cs.total() > 0 && cs.passed() == cs.total();
        String  color   = allPass ? Styles.SUCCESS : Styles.DANGER;
        String  symbol  = allPass ? "✓" : "✗";

        String title = String.format("%s — %.1f / %.0f pts  (%d/%d)  %s",
                cs.label(), cs.earned(), cs.maxPts(), cs.passed(), cs.total(), symbol);

        // ── Per-test content ──────────────────────────────────────────────────
        VBox content = new VBox(4);
        content.setPadding(new Insets(8, 12, 8, 12));

        for (Grader.TestResult tr : cs.tests()) {
            Label lbl;
            if (tr.passed()) {
                lbl = Styles.lbl("✓  " + tr.methodName(), Styles.SUCCESS, 12, false);
            } else {
                String shortErr = tr.errorMsg().isEmpty()
                        ? ""
                        : "  — " + abbreviate(tr.errorMsg(), 60);
                lbl = new Label("✗  " + tr.methodName() + shortErr);
                lbl.setStyle(Styles.ss(Styles.DANGER, 12, false) + "-fx-font-style:italic;");
                if (!tr.errorMsg().isEmpty()) {
                    Tooltip tt = new Tooltip(tr.errorMsg());
                    tt.setWrapText(true);
                    tt.setMaxWidth(480);
                    Tooltip.install(lbl, tt);
                }
            }
            content.getChildren().add(lbl);
        }

        if (cs.tests().isEmpty()) {
            content.getChildren().add(Styles.lbl("Aucun test trouvé.", Styles.MUTED, 12, false));
        }

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setPrefHeight(Math.min(cs.tests().size() * 26 + 24, 280));
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        TitledPane pane = new TitledPane(title, sp);
        pane.setStyle(Styles.ss(color, 13, true));
        return pane;
    }

    private void showError(String msg) {
        statusLbl.setText("Erreur : " + (msg != null ? msg : "inconnue"));
        statusLbl.setStyle(Styles.ss(Styles.DANGER, 12, false));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Walks up from CWD (up to 5 levels) to find the directory containing pom.xml. */
    private static File findProjectRoot() throws java.io.IOException {
        File dir = new File(".").getAbsoluteFile();
        for (int i = 0; i < 5; i++) {
            if (new File(dir, "pom.xml").exists()) return dir;
            File parent = dir.getParentFile();
            if (parent == null) break;
            dir = parent;
        }
        throw new java.io.IOException("pom.xml introuvable (jusqu'à 5 niveaux de parent)");
    }

    /** Trims a Maven output line to at most {@code max} characters. */
    private static String abbreviate(String s, int max) {
        if (s == null) return "";
        s = s.strip();
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
