package ca.udem.ift2015.autocompleter.model;

import java.util.List;

/**
 * Trie (arbre préfixe) associant chaque mot à sa fréquence cumulée.
 *
 * <p>Un {@code Trie} permet d'insérer des mots avec leurs fréquences, de les
 * rechercher en O(L) (L = longueur du mot), et de compléter un préfixe en
 * retournant les k mots les plus fréquents du sous-arbre correspondant.</p>
 *
 * <p><b>Règle de tri pour {@link #complete} :</b></p>
 * <ol>
 *   <li>Fréquence <em>décroissante</em> (le mot le plus fréquent en premier).</li>
 *   <li>En cas d'égalité de fréquence : ordre <em>lexicographique croissant</em>.</li>
 * </ol>
 *
 * <p><b>Exemple :</b></p>
 * <pre>{@code
 * Trie trie = new PrefixTrie();
 * trie.insert("the",  3);
 * trie.insert("they", 2);
 * trie.insert("cat",  5);
 * trie.search("the");          // → 3
 * trie.search("cat");          // → 5
 * trie.search("dog");          // → 0
 * trie.complete("th", 2);      // → ["the", "they"]
 * trie.size();                  // → 3
 * }</pre>
 *
 * @see ca.udem.ift2015.autocompleter.student.PrefixTrie
 */
public interface Trie {

    /**
     * Insère un mot dans le trie en ajoutant {@code frequency} à sa fréquence cumulée.
     *
     * <p>Si le mot est déjà présent, sa fréquence est incrémentée de {@code frequency}
     * (les insertions successives s'accumulent). Si le mot est nouveau, les nœuds
     * manquants sont créés et {@link #size()} est incrémenté de 1.</p>
     *
     * <p><b>Complexité attendue :</b> O(L) où L est la longueur du mot.</p>
     *
     * @param word      le mot à insérer (non nul, non vide)
     * @param frequency la fréquence à ajouter (≥ 1)
     */
    void insert(String word, int frequency);

    /**
     * Insère un mot dans le trie en incrémentant sa fréquence de 1.
     *
     * <p>Équivalent à {@code insert(word, 1)}.</p>
     *
     * @param word le mot à insérer (non nul, non vide)
     */
    default void insert(String word) {
        insert(word, 1);
    }

    /**
     * Retourne la fréquence cumulée du mot, ou {@code 0} s'il est absent.
     *
     * <p>Un mot est considéré absent si le chemin dans le trie n'existe pas,
     * ou si le nœud terminal n'est pas marqué comme fin de mot.</p>
     *
     * <p><b>Complexité attendue :</b> O(L) où L est la longueur du mot.</p>
     *
     * @param word le mot à rechercher (non nul)
     * @return la fréquence cumulée du mot, ou {@code 0} si inconnu
     */
    int search(String word);

    /**
     * Retourne les {@code k} mots du trie commençant par {@code prefix},
     * triés par fréquence décroissante (à égalité : ordre lexicographique croissant).
     *
     * <p>Si le préfixe est absent du trie, retourne une liste vide.
     * Si {@code k ≤ 0}, retourne une liste vide.
     * Si moins de {@code k} mots correspondent, retourne tous les mots correspondants.</p>
     *
     * <p><b>Complexité attendue :</b> O(L + M log k) où L est la longueur du préfixe
     * et M le nombre de mots dans le sous-trie.</p>
     *
     * @param prefix le préfixe à compléter (non nul ; chaîne vide → tous les mots)
     * @param k      le nombre maximum de résultats (≥ 0)
     * @return liste de mots triée par fréquence décroissante
     */
    List<String> complete(String prefix, int k);

    /**
     * Retourne le nombre de mots distincts insérés dans le trie.
     *
     * @return nombre de mots distincts (0 si le trie est vide)
     */
    int size();

    /**
     * Retourne le nombre de nœuds créés dans le trie, hors racine.
     *
     * <p>Chaque nouveau caractère qui n'avait pas encore de nœud correspondant
     * incrémente ce compteur. Un caractère partagé par plusieurs mots ne compte
     * qu'une seule fois.</p>
     *
     * @return nombre de nœuds internes (0 si le trie est vide)
     */
    int nodeCount();
}
