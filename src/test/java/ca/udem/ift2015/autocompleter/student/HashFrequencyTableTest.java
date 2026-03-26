package ca.udem.ift2015.autocompleter.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour {@link HashFrequencyTable}.
 *
 * <p>Ces tests vérifient le comportement attendu de votre implémentation.
 * Tous doivent passer une fois les TODOs 1-6 complétés.</p>
 */
class HashFrequencyTableTest {

    private HashFrequencyTable table;

    @BeforeEach
    void setUp() {
        table = new HashFrequencyTable();
    }

    // -------------------------------------------------------------------------
    // État initial
    // -------------------------------------------------------------------------

    @Test
    void vocabularyIsEmptyOnFreshTable() {
        assertTrue(table.vocabulary().isEmpty(),
                "vocabulary() sur une table fraîche doit retourner un ensemble vide");
    }

    @Test
    void getOnAbsentTokenReturnsZero() {
        assertEquals(0, table.get("absent"),
                "get() sur un token absent doit retourner 0, pas une exception");
    }

    // -------------------------------------------------------------------------
    // increment / get
    // -------------------------------------------------------------------------

    @Test
    void incrementOnceYieldsCountOne() {
        table.increment("chat");
        assertEquals(1, table.get("chat"));
    }

    @Test
    void incrementMultipleTimesAccumulates() {
        table.increment("chat");
        table.increment("chat");
        table.increment("chat");
        assertEquals(3, table.get("chat"));
    }

    @Test
    void incrementDistinctTokensAreIndependent() {
        table.increment("chat");
        table.increment("chat");
        table.increment("chien");
        assertEquals(2, table.get("chat"));
        assertEquals(1, table.get("chien"));
        assertEquals(0, table.get("lapin"), "token non incrémenté → 0");
    }

    // -------------------------------------------------------------------------
    // total
    // -------------------------------------------------------------------------

    @Test
    void totalCountsAllIncrements() {
        table.increment("chat");
        table.increment("chat");
        table.increment("chien");
        assertEquals(3, table.total(),
                "total() doit être la somme de tous les comptes");
    }

    @Test
    void totalAfterNoIncrementIsZero() {
        assertEquals(0, table.total());
    }

    // -------------------------------------------------------------------------
    // vocabulary
    // -------------------------------------------------------------------------

    @Test
    void vocabularyContainsAllIncrementedTokens() {
        table.increment("chat");
        table.increment("chien");
        table.increment("chat"); // doublon : ne doit pas dupliquer dans le vocab
        Set<String> vocab = table.vocabulary();
        assertEquals(2, vocab.size());
        assertTrue(vocab.contains("chat"));
        assertTrue(vocab.contains("chien"));
    }

    @Test
    void vocabularyDoesNotContainAbsentToken() {
        table.increment("chat");
        assertFalse(table.vocabulary().contains("lapin"));
    }

    // -------------------------------------------------------------------------
    // isEmpty
    // -------------------------------------------------------------------------

    @Test
    void isEmptyAfterIncrementReturnsFalse() {
        table.increment("chat");
        assertFalse(table.isEmpty());
    }

    @Test
    void isEmptyOnFreshTableReturnsTrue() {
        assertTrue(table.isEmpty());
    }

    // -------------------------------------------------------------------------
    // incrementBy
    // -------------------------------------------------------------------------

    @Test
    void incrementByInitializesAbsentToken() {
        table.incrementBy("chat", 5);
        assertEquals(5, table.get("chat"));
    }

    @Test
    void incrementByAccumulatesOnExistingToken() {
        table.increment("chat");       // count = 1
        table.incrementBy("chat", 4); // count = 5
        assertEquals(5, table.get("chat"));
    }

    @Test
    void incrementByUpdatesTotalCount() {
        table.incrementBy("chat",  3);
        table.incrementBy("chien", 2);
        assertEquals(5, table.total());
    }

    @Test
    void incrementByAddsTokenToVocabulary() {
        table.incrementBy("lapin", 7);
        assertTrue(table.vocabulary().contains("lapin"));
    }

    @Test
    void incrementByWithOneEquivalentToIncrement() {
        table.increment("chat");
        table.incrementBy("chien", 1);
        assertEquals(table.get("chat"), table.get("chien"));
    }
}
