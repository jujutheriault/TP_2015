package ca.udem.ift2015.autocompleter.student;

import ca.udem.ift2015.autocompleter.model.NGramModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour {@link KatzBackoffModel}.
 *
 * <p>Ces tests vérifient l'entraînement (uni/bi/trigrammes), le backoff de Katz,
 * la prédiction varargs et la complétion de préfixe.
 * Tous doivent passer une fois les TODOs 10-13 complétés.</p>
 *
 * <p><b>Note :</b> ces tests dépendent aussi de {@link HashFrequencyTable},
 * {@link HeapTopKStrategy} et {@link PrefixTrie}.</p>
 */
class KatzBackoffModelTest {

    private NGramModel model;

    /** Construit une liste de phrases à partir d'un tableau de tableaux. */
    private static List<List<String>> sentences(String[]... ss) {
        List<List<String>> result = new java.util.ArrayList<>();
        for (String[] s : ss) result.add(List.of(s));
        return result;
    }

    @BeforeEach
    void setUp() {
        model = new KatzBackoffModel(new HeapTopKStrategy());
    }

    // -------------------------------------------------------------------------
    // Avant entraînement
    // -------------------------------------------------------------------------

    @Test
    void predictOnUntrained_returnsNull() {
        assertNull(model.predict("the"),
                "Sans entraînement, predict() doit retourner null");
    }

    @Test
    void topKOnUntrained_returnsEmpty() {
        assertTrue(model.topK(3, "the").isEmpty());
    }

    @Test
    void countsZeroBeforeTraining() {
        assertEquals(0, model.unigramCount());
        assertEquals(0, model.bigramCount());
        assertEquals(0, model.trigramCount());
    }

    // -------------------------------------------------------------------------
    // Comptage après entraînement
    // -------------------------------------------------------------------------

    @Test
    void unigramCountAfterTraining() {
        // "the old man sat" → 4 types
        model.train(sentences(new String[]{"the", "old", "man", "sat"}));
        assertEquals(4, model.unigramCount());
    }

    @Test
    void bigramCountAfterTraining() {
        // "the old man sat" → 3 bigrammes : (the,old), (old,man), (man,sat)
        model.train(sentences(new String[]{"the", "old", "man", "sat"}));
        assertEquals(3, model.bigramCount());
    }

    @Test
    void trigramCountAfterTraining() {
        // "the old man sat" → 2 trigrammes : (the old,man), (old man,sat)
        model.train(sentences(new String[]{"the", "old", "man", "sat"}));
        assertEquals(2, model.trigramCount());
    }

    @Test
    void trainingAccumulatesAcrossSentences() {
        model.train(sentences(new String[]{"the", "cat"}));
        model.train(sentences(new String[]{"the", "dog"}));
        // the(×2), cat(×1), dog(×1) → 3 unigrammes
        assertEquals(3, model.unigramCount());
    }

    @Test
    void sentenceBoundaryResetsContext() {
        // Deux phrases séparées : pas de bigramme (cat → dog) ni trigramme
        model.train(sentences(
                new String[]{"the", "cat"},
                new String[]{"dog", "ran"}
        ));
        // Bigrammes : (the,cat), (dog,ran) → 2
        assertEquals(2, model.bigramCount());
        // Trigrammes : aucun (phrases de 2 mots) → 0
        assertEquals(0, model.trigramCount());
    }

    // -------------------------------------------------------------------------
    // topK — backoff trigramme
    // -------------------------------------------------------------------------

    @Test
    void topKUsesTrigramWhenAvailable() {
        model.train(sentences(
                new String[]{"the", "old", "man"},
                new String[]{"the", "old", "man"},
                new String[]{"the", "old", "woman"}
        ));
        // trigram "the old" → man×2, woman×1
        List<String> result = model.topK(2, "the", "old");
        assertEquals(2, result.size());
        assertEquals("man", result.get(0));
        assertEquals("woman", result.get(1));
    }

    @Test
    void topKBackoffToBigramWhenNoTrigram() {
        model.train(sentences(
                new String[]{"the", "old", "man"},
                new String[]{"old", "cat"},
                new String[]{"old", "cat"},
                new String[]{"old", "dog"}
        ));
        // Pas de trigram "xx old" → backoff sur bigram "old" → cat×2, dog×1
        List<String> result = model.topK(2, "xx", "old");
        assertEquals(2, result.size());
        assertEquals("cat", result.get(0));
        assertEquals("dog", result.get(1));
    }

    @Test
    void topKBackoffToUnigramWhenNoBigram() {
        model.train(sentences(
                new String[]{"cat", "cat", "cat", "dog", "dog", "bird"}
        ));
        // Contexte inconnu "zzz" → unigrammes : cat×3, dog×2, bird×1
        List<String> result = model.topK(2, "zzz");
        assertEquals(List.of("cat", "dog"), result);
    }

    @Test
    void topKNoContext_usesUnigrams() {
        model.train(sentences(
                new String[]{"cat", "cat", "dog", "bird"}
        ));
        List<String> result = model.topK(2);
        assertEquals(2, result.size());
        assertEquals("cat", result.get(0));
    }

    @Test
    void topKZeroReturnsEmpty() {
        model.train(sentences(new String[]{"cat"}));
        assertTrue(model.topK(0, "cat").isEmpty());
    }

    @Test
    void topKKLargerThanVocab() {
        model.train(sentences(new String[]{"the", "cat"}));
        // bigram "the" → {cat×1} → 1 seul résultat même si k=10
        List<String> result = model.topK(10, "the");
        assertEquals(1, result.size());
        assertEquals("cat", result.get(0));
    }

    @Test
    void strictBackoffTrigramNotFilledFromBigram() {
        model.train(sentences(
                new String[]{"the", "old", "man"},   // trigram "the old" → {man:1}
                new String[]{"old", "cat"},
                new String[]{"old", "dog"}            // bigram  "old"     → {cat:1, dog:1}
        ));
        // trigram "the old" existe mais n'a qu'un seul successeur :
        // le repli vers le bigramme "old" est interdit (repli strict)
        List<String> result = model.topK(5, "the", "old");
        assertEquals(1, result.size(), "Le repli strict interdit de compléter depuis un niveau inférieur");
        assertEquals("man", result.get(0));
    }

    @Test
    void topKDoubleBackoffToUnigram() {
        model.train(sentences(new String[]{"cat", "cat", "cat", "dog", "dog", "bird"}));
        // Contexte à deux mots : ni trigram "zzz yyy" ni bigram "yyy" n'existent
        // → double repli jusqu'aux unigrammes
        List<String> result = model.topK(3, "zzz", "yyy");
        assertEquals(List.of("cat", "dog", "bird"), result);
    }

    @Test
    void trainingAccumulatesBigramCount() {
        model.train(sentences(new String[]{"the", "cat"})); // bigramme (the→cat)
        model.train(sentences(new String[]{"the", "dog"})); // bigramme (the→dog)
        // Table "the" : {cat×1, dog×1} → 2 successeurs distincts
        assertEquals(2, model.bigramCount());
    }

    @Test
    void frequencyReturnsUnigramCount() {
        model.train(sentences(new String[]{"cat", "cat", "dog"}));
        assertEquals(2, model.frequency("cat"), "cat apparaît 2 fois");
        assertEquals(1, model.frequency("dog"), "dog apparaît 1 fois");
        assertEquals(0, model.frequency("bird"), "bird absent → 0");
    }

    // -------------------------------------------------------------------------
    // predict — varargs
    // -------------------------------------------------------------------------

    @Test
    void predictTwoWords_trigram() {
        model.train(sentences(
                new String[]{"the", "old", "man"},
                new String[]{"the", "old", "man"},
                new String[]{"the", "old", "woman"}
        ));
        assertEquals("man", model.predict("the", "old"));
    }

    // -------------------------------------------------------------------------
    // complete
    // -------------------------------------------------------------------------

    @Test
    void completeReturnsWordsWithPrefix() {
        model.train(sentences(
                new String[]{"the", "they", "their", "there", "cat"}
        ));
        List<String> result = model.complete("th", 10);
        assertTrue(result.containsAll(List.of("the", "they", "their", "there")),
                "complete('th') doit retourner tous les mots commençant par 'th'");
        assertFalse(result.contains("cat"));
    }

    @Test
    void completeRankedByFrequency() {
        model.train(sentences(
                new String[]{"the", "the", "the", "they", "they", "their"}
        ));
        List<String> result = model.complete("th", 3);
        assertEquals("the", result.get(0), "the (×3) doit être premier");
        assertEquals("they", result.get(1), "they (×2) doit être deuxième");
        assertEquals("their", result.get(2));
    }

    @Test
    void completeUnknownPrefix_returnsEmpty() {
        model.train(sentences(new String[]{"cat", "dog"}));
        assertTrue(model.complete("xyz", 5).isEmpty());
    }

    @Test
    void completeKZero_returnsEmpty() {
        model.train(sentences(new String[]{"cat", "dog"}));
        assertTrue(model.complete("c", 0).isEmpty());
    }

    @Test
    void completeKLargerThanMatches() {
        model.train(sentences(new String[]{"cat", "car", "cap"}));
        List<String> result = model.complete("ca", 100);
        assertEquals(3, result.size());
    }
}
