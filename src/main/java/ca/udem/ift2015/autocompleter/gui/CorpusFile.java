package ca.udem.ift2015.autocompleter.gui;

import javafx.beans.property.*;

import java.io.File;

/**
 * Représente un fichier corpus avec ses statistiques de n-grammes.
 */
class CorpusFile {

    final File            file;
    final BooleanProperty selected     = new SimpleBooleanProperty(false);
    final IntegerProperty unigramCount = new SimpleIntegerProperty(-1);
    final IntegerProperty bigramCount  = new SimpleIntegerProperty(-1);
    final IntegerProperty trigramCount = new SimpleIntegerProperty(-1);

    CorpusFile(File file) { this.file = file; }

    String getSize() {
        long b = file.length();
        if (b < 1_024)     return b + " B";
        if (b < 1_048_576) return (b / 1_024) + " KB";
        return String.format("%.1f MB", b / 1_048_576.0);
    }

    boolean isTrained() { return unigramCount.get() >= 0; }

    void resetStats() {
        selected    .set(false);
        unigramCount.set(-1);
        bigramCount .set(-1);
        trigramCount.set(-1);
    }
}
