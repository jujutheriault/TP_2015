package ca.udem.ift2015.autocompleter.student;

import ca.udem.ift2015.autocompleter.model.FrequencyTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour {@link HeapTopKStrategy}.
 *
 * <p>Ces tests vérifient à la fois le classement par fréquence et le bris
 * d'égalité lexicographique. Tous doivent passer une fois le TODO 6 complété.</p>
 */
class HeapTopKStrategyTest {

    private HeapTopKStrategy strategy;

    /** Construit une FrequencyTable à partir de paires (token, compte). */
    private static FrequencyTable tableOf(Object... pairs) {
        HashFrequencyTable t = new HashFrequencyTable();
        for (int i = 0; i < pairs.length; i += 2) {
            String token = (String) pairs[i];
            int count = (int) pairs[i + 1];
            for (int j = 0; j < count; j++) t.increment(token);
        }
        return t;
    }

    @BeforeEach
    void setUp() {
        strategy = new HeapTopKStrategy();
    }

    // -------------------------------------------------------------------------
    // Cas limites
    // -------------------------------------------------------------------------

    @Test
    void kZeroReturnsEmptyList() {
        FrequencyTable t = tableOf("chat", 5, "chien", 3);
        assertEquals(List.of(), strategy.topK(t, 0));
    }

    @Test
    void emptyTableReturnsEmptyList() {
        FrequencyTable t = new HashFrequencyTable();
        assertEquals(List.of(), strategy.topK(t, 5));
    }

    @Test
    void kGreaterThanVocabReturnsAllTokens() {
        FrequencyTable t = tableOf("chat", 5, "chien", 3);
        List<String> result = strategy.topK(t, 10);
        assertEquals(2, result.size(), "k > vocab : retourner tous les tokens");
        assertEquals("chat", result.get(0));
        assertEquals("chien", result.get(1));
    }

    // -------------------------------------------------------------------------
    // Classement par fréquence
    // -------------------------------------------------------------------------

    @Test
    void resultsAreOrderedByDescendingFrequency() {
        FrequencyTable t = tableOf("lapin", 1, "chat", 5, "chien", 3);
        List<String> result = strategy.topK(t, 3);
        assertEquals(List.of("chat", "chien", "lapin"), result,
                "Ordre décroissant de fréquence attendu");
    }

    @Test
    void topOneReturnsHighestFrequencyToken() {
        FrequencyTable t = tableOf("a", 2, "b", 9, "c", 4);
        List<String> result = strategy.topK(t, 1);
        assertEquals(List.of("b"), result);
    }

    // -------------------------------------------------------------------------
    // Bris d'égalité lexicographique
    // -------------------------------------------------------------------------

    @Test
    void tiebreakIsLexicographicAscending() {
        // chat, chien, lapin ont tous la même fréquence
        FrequencyTable t = tableOf("lapin", 5, "chien", 5, "chat", 5);
        List<String> result = strategy.topK(t, 3);
        assertEquals(List.of("chat", "chien", "lapin"), result,
                "À fréquence égale : ordre lexicographique croissant");
    }

    @Test
    void tiebreakInTopK() {
        // souris(8) premier ; chat(5) et chien(5) à égalité ; lapin(3) exclu
        FrequencyTable t = tableOf("souris", 8, "lapin", 3, "chien", 5, "chat", 5);
        List<String> result = strategy.topK(t, 3);
        assertEquals(List.of("souris", "chat", "chien"), result);
    }

    @Test
    void singleTokenTable() {
        FrequencyTable t = tableOf("mot", 7);
        assertEquals(List.of("mot"), strategy.topK(t, 5));
    }

    // -------------------------------------------------------------------------
    // Cas limites supplémentaires
    // -------------------------------------------------------------------------

    @Test
    void kNegativeReturnsEmptyList() {
        FrequencyTable t = tableOf("chat", 5);
        assertEquals(List.of(), strategy.topK(t, -1),
                "k négatif doit retourner une liste vide comme k = 0");
    }

    @Test
    void tiebreakBetweenTwoTokensOnly() {
        // Cas minimal du bris d'égalité : seulement deux tokens de même fréquence
        FrequencyTable t = tableOf("zebra", 5, "apple", 5);
        assertEquals(List.of("apple", "zebra"), strategy.topK(t, 2),
                "À fréquence égale entre deux tokens : ordre lexicographique croissant");
    }
}
