package ca.udem.ift2015.autocompleter.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour {@link PrefixTrie}.
 *
 * <p>Ces tests vérifient l'insertion, la recherche et la complétion de préfixe.
 * Tous doivent passer une fois les TODOs 7, 8 et 9 complétés.</p>
 */
class PrefixTrieTest {

    private PrefixTrie trie;

    @BeforeEach
    void setUp() {
        trie = new PrefixTrie();
    }

    // -------------------------------------------------------------------------
    // insert + search
    // -------------------------------------------------------------------------

    @Test
    void searchOnEmptyTrie_returnsZero() {
        // Trie fraîchement créé, aucun insert
        assertEquals(0, trie.search("cat"),
                "search() sur un trie vide doit retourner 0 sans exception");
    }

    @Test
    void insertAndSearchSingleWord() {
        trie.insert("cat", 5);
        assertEquals(5, trie.search("cat"));
    }

    @Test
    void searchAbsentWord_returnsZero() {
        trie.insert("cat", 3);
        assertEquals(0, trie.search("dog"));
    }

    @Test
    void searchPrefixOnly_returnsZero() {
        trie.insert("cat", 3);
        // "ca" n'est pas un mot inséré
        assertEquals(0, trie.search("ca"));
    }

    @Test
    void insertSameWordAccumulatesFrequency() {
        trie.insert("cat", 3);
        trie.insert("cat", 2);
        assertEquals(5, trie.search("cat"), "Les insertions multiples doivent s'accumuler");
    }

    @Test
    void sizeCountsDistinctWords() {
        trie.insert("cat", 1);
        trie.insert("car", 1);
        trie.insert("cat", 1); // doublon
        assertEquals(2, trie.size());
    }

    @Test
    void nodeCountGrowsWithNewChars() {
        trie.insert("cat", 1); // 3 nœuds : c, a, t
        assertEquals(3, trie.nodeCount());
        trie.insert("car", 1); // 1 nouveau nœud : r (c et a partagés)
        assertEquals(4, trie.nodeCount());
    }

    @Test
    void nodeCountAfterInsertingPrefixOfExistingWord() {
        trie.insert("cat", 1); // nœuds : c-a-t → 3
        trie.insert("ca", 1);  // "ca" est un préfixe de "cat" : aucun nouveau nœud
        assertEquals(3, trie.nodeCount(),
                "Insérer un préfixe d'un mot existant ne doit pas créer de nouveaux nœuds");
        assertEquals(2, trie.size(), "Deux mots distincts : \"ca\" et \"cat\"");
        assertEquals(1, trie.search("ca"),  "\"ca\" est désormais un mot inséré");
        assertEquals(1, trie.search("cat"), "\"cat\" reste inchangé");
    }

    // -------------------------------------------------------------------------
    // complete
    // -------------------------------------------------------------------------

    @Test
    void completeReturnsAllMatchesForPrefix() {
        trie.insert("the", 3);
        trie.insert("they", 2);
        trie.insert("their", 1);
        trie.insert("cat", 5);

        List<String> result = trie.complete("th", 10);
        assertEquals(3, result.size());
        assertTrue(result.containsAll(List.of("the", "they", "their")));
        assertFalse(result.contains("cat"));
    }

    @Test
    void completeRankedByDescendingFrequency() {
        trie.insert("the", 10);
        trie.insert("they", 5);
        trie.insert("their", 2);

        List<String> result = trie.complete("th", 3);
        assertEquals("the", result.get(0));
        assertEquals("they", result.get(1));
        assertEquals("their", result.get(2));
    }

    @Test
    void completeTiebreakLexicographic() {
        trie.insert("abc", 5);
        trie.insert("abd", 5);
        trie.insert("abe", 5);

        List<String> result = trie.complete("ab", 3);
        assertEquals(List.of("abc", "abd", "abe"), result,
                "À fréquence égale : ordre lexicographique croissant");
    }

    @Test
    void completeKSmallerThanMatches() {
        trie.insert("the", 10);
        trie.insert("they", 5);
        trie.insert("their", 2);
        trie.insert("there", 8);

        List<String> result = trie.complete("th", 2);
        assertEquals(2, result.size());
        assertEquals("the", result.get(0));
        assertEquals("there", result.get(1));
    }

    @Test
    void completeKLargerThanMatches() {
        trie.insert("cat", 3);
        trie.insert("car", 1);

        List<String> result = trie.complete("ca", 100);
        assertEquals(2, result.size());
    }

    @Test
    void completeAbsentPrefix_returnsEmpty() {
        trie.insert("cat", 1);
        assertTrue(trie.complete("xyz", 5).isEmpty());
    }

    @Test
    void completeKZero_returnsEmpty() {
        trie.insert("cat", 1);
        assertTrue(trie.complete("ca", 0).isEmpty());
    }

    @Test
    void completeEmptyPrefix_returnsTopK() {
        trie.insert("the", 10);
        trie.insert("cat", 3);
        trie.insert("dog", 7);

        List<String> result = trie.complete("", 2);
        assertEquals(2, result.size());
        assertEquals("the", result.get(0));
        assertEquals("dog", result.get(1));
    }

    @Test
    void completeExactWord_returnsItself() {
        trie.insert("hello", 4);
        List<String> result = trie.complete("hello", 5);
        assertEquals(List.of("hello"), result);
    }

    @Test
    void completeWhenPrefixIsWordWithExtensions() {
        // "cat" est à la fois un mot inséré et un préfixe de "catch" et "cats"
        trie.insert("cat",   3);
        trie.insert("catch", 2);
        trie.insert("cats",  1);
        List<String> result = trie.complete("cat", 10);
        assertEquals(3, result.size(),
                "Le mot exact du préfixe doit être inclus quand il a aussi des extensions");
        assertEquals("cat",   result.get(0), "cat (freq 3) doit être premier");
        assertEquals("catch", result.get(1), "catch (freq 2) doit être deuxième");
        assertEquals("cats",  result.get(2), "cats (freq 1) doit être troisième");
    }
}
