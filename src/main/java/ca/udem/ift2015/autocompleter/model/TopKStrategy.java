package ca.udem.ift2015.autocompleter.model;

import java.util.List;

/**
 * Stratégie de sélection des {@code k} tokens les plus fréquents d'une table.
 *
 * <p>Cette interface encapsule l'algorithme de classement utilisé par {@link NGramModel}
 * pour répondre aux requêtes {@code topK}. L'implémentation attendue utilise un
 * <b>min-tas (PriorityQueue) de taille k</b>, ce qui donne une complexité
 * O(n&nbsp;log&nbsp;k) au lieu de O(n&nbsp;log&nbsp;n) pour un tri complet
 * (n = taille du vocabulaire).</p>
 *
 * <p><b>Règle de tri du résultat :</b></p>
 * <ol>
 *   <li>Fréquence <em>décroissante</em> (le token le plus fréquent est en premier).</li>
 *   <li>En cas d'égalité de fréquence : ordre <em>lexicographique croissant</em>
 *       ({@link String#compareTo}).</li>
 * </ol>
 *
 * <p><b>Exemple :</b> table = {chat→5, chien→5, lapin→3, souris→8}, k=3</p>
 * <pre>
 * résultat → ["souris"(8), "chat"(5), "chien"(5)]
 * </pre>
 *
 * @see ca.udem.ift2015.autocompleter.student.HeapTopKStrategy
 */
public interface TopKStrategy {

    /**
     * Retourne les {@code k} tokens les plus fréquents de {@code table}.
     *
     * <p>Si {@code table} contient moins de {@code k} tokens distincts, retourne
     * tous les tokens triés. Si {@code table} est vide ou {@code k ≤ 0},
     * retourne une liste vide.</p>
     *
     * <p><b>Complexité attendue :</b> O(n log k) où n = {@code table.vocabulary().size()}.</p>
     *
     * @param table la table de fréquences à consulter (non nulle)
     * @param k     le nombre maximum de résultats souhaités (≥ 0)
     * @return liste immuable de tokens, triés par fréquence décroissante puis
     *         ordre lexicographique croissant en cas d'égalité
     */
    List<String> topK(FrequencyTable table, int k);
}
