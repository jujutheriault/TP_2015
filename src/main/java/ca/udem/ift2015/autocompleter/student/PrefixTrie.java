package ca.udem.ift2015.autocompleter.student;

import ca.udem.ift2015.autocompleter.model.FrequencyTable;
import ca.udem.ift2015.autocompleter.model.Trie;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Trie (arbre préfixe) associant chaque mot à sa fréquence.
 *
 * <p>Chaque nœud contient une map {@code Character → TrieNode},
 * un booléen {@code isEndOfWord} et un entier {@code frequency}.
 */
public class PrefixTrie implements Trie {

    private static class TrieNode {
        private final HashMap<Character, TrieNode> children = new HashMap<>();
        private boolean isEndOfWord = false;
        private int frequency = 0;
    }

    private final TrieNode root = new TrieNode();
    private int size = 0;       // nombre de mots distincts
    private int nodeCount = 0;  // nombre de nœuds créés (hors racine)

    public int size()      { return size; }
    public int nodeCount() { return nodeCount; }

    /**
     * TODO 8 — Insérer un mot en incrémentant sa fréquence de {@code frequency}.
     *
     * <p>Descendre caractère par caractère. Pour chaque caractère absent,
     * créer le nœud fils et incrémenter {@code nodeCount}. Au nœud terminal :
     * si c'est la première insertion, mettre {@code isEndOfWord = true} et
     * incrémenter {@code size} ; dans tous les cas, ajouter {@code frequency}
     * à {@code node.frequency}.
     */
    public void insert(String word, int frequency) {
        throw new UnsupportedOperationException("TODO 8 — insert non implémenté");
    }

    /**
     * TODO 9 — Retourner la fréquence du mot, ou 0 s'il est absent.
     *
     * <p>Descendre caractère par caractère ; retourner 0 dès qu'un nœud
     * est manquant. Au nœud terminal, retourner {@code node.frequency}
     * seulement si {@code node.isEndOfWord} est vrai.
     */
    public int search(String word) {
        throw new UnsupportedOperationException("TODO 9 — search non implémenté");
    }

    /**
     * TODO 10 — Retourner les k mots commençant par {@code prefix},
     * triés par fréquence décroissante (à égalité : ordre lexicographique croissant).
     *
     * <p>Étapes :
     * <ol>
     *   <li>Si k ≤ 0, retourner une liste vide.</li>
     *   <li>Descendre jusqu'au nœud correspondant au préfixe ;
     *       si absent, retourner une liste vide.</li>
     *   <li>Appeler {@link #dfs} pour obtenir une {@link FrequencyTable}
     *       de tous les mots du sous-trie.</li>
     *   <li>Déléguer à {@code new HeapTopKStrategy().topK(table, k)}.</li>
     * </ol>
     */
    public List<String> complete(String prefix, int k) {
        throw new UnsupportedOperationException("TODO 10 — complete non implémenté");
    }

    /**
     * Parcours DFS du sous-trie enraciné en {@code node}.
     * Retourne une {@link FrequencyTable} contenant tous les mots du sous-trie
     * avec leurs fréquences cumulées.
     *
     * <p>Ne pas modifier cette méthode.
     */
    private FrequencyTable dfs(TrieNode node, StringBuilder current) {
        FrequencyTable table = new HashFrequencyTable();
        collectWords(node, current, table);
        return table;
    }

    private void collectWords(TrieNode node, StringBuilder sb, FrequencyTable table) {
        if (node.isEndOfWord) table.incrementBy(sb.toString(), node.frequency);
        for (var entry : node.children.entrySet()) {
            sb.append(entry.getKey());
            collectWords(entry.getValue(), sb, table);
            sb.deleteCharAt(sb.length() - 1);
        }
    }
}
