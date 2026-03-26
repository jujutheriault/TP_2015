package ca.udem.ift2015.autocompleter.preprocessing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * NE PAS MODIFIER CETTE CLASSE<br>
 * Transforme un flux de texte brut en une liste de phrases tokenisées.
 *
 * <h2>Contrat de sortie</h2>
 * <ul>
 *   <li>Retourne une {@code List<List<String>>} : chaque sous-liste est une phrase.</li>
 *   <li>Tous les tokens sont en <b>minuscules</b>.</li>
 *   <li>Seuls les caractères alphabétiques ({@link Character#isLetter}) sont conservés.</li>
 *   <li>La ponctuation ({@code .} {@code !} {@code ?}) délimite les phrases ;
 *       les autres caractères non-alphabétiques servent de séparateurs de tokens.</li>
 *   <li>Les phrases vides (sans aucun token) sont ignorées.</li>
 * </ul>
 *
 * <h2>Exemple</h2>
 * <pre>
 * Entrée  : "The cat sat. A dog runs!"
 * Sortie  : [["the", "cat", "sat"], ["a", "dog", "runs"]]
 * </pre>
 *
 */
public class TextTokenizer {

    private final Reader reader;

    /**
     * Crée un tokeniseur lisant depuis {@code reader}.
     *
     * @param reader la source de texte (non nul) ; sera fermé après {@link #tokenize()}
     */
    public TextTokenizer(Reader reader) {
        this.reader = reader;
    }

    /**
     * Lit le flux entier et retourne la liste des phrases tokenisées.
     *
     * <p>Le {@code reader} est fermé à la fin de cette méthode.</p>
     *
     * @return liste de phrases ; chaque phrase est une liste de tokens non vides
     */
    public List<List<String>> tokenize() {
        List<List<String>> sentences = new ArrayList<>();
        List<String> current = new ArrayList<>();

        try (BufferedReader br = reader instanceof BufferedReader b
                ? b : new BufferedReader(reader)) {

            String line;
            while ((line = br.readLine()) != null) {
                StringBuilder word = new StringBuilder();

                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);

                    if (Character.isLetter(c)) {
                        word.append(Character.toLowerCase(c));
                    } else {
                        if (!word.isEmpty()) {
                            current.add(word.toString());
                            word.setLength(0);
                        }
                        if (c == '.' || c == '!' || c == '?') {
                            if (!current.isEmpty()) {
                                sentences.add(current);
                                current = new ArrayList<>();
                            }
                        }
                    }
                }

                // Dernier mot de la ligne sans ponctuation finale
                if (!word.isEmpty()) {
                    current.add(word.toString());
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Dernière phrase sans ponctuation finale
        if (!current.isEmpty()) {
            sentences.add(current);
        }

        return sentences;
    }
}
