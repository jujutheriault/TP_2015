package ca.udem.ift2015.autocompleter.model;

import java.util.Set;

/**
 * Table de fréquences associant des tokens à leur nombre d'occurrences.
 *
 * <p>Une {@code FrequencyTable} permet de compter les occurrences de tokens (chaînes de
 * caractères) et de consulter ces comptes. Elle est utilisée à deux niveaux dans le modèle
 * de langage :</p>
 * <ul>
 *   <li><b>Unigrammes</b> : une table globale compte chaque token du corpus.</li>
 *   <li><b>Bigrammes</b> : pour chaque contexte {@code u}, une table distincte compte
 *       chaque successeur {@code v} observé après {@code u}.</li>
 * </ul>
 *
 * <p><b>Exemple d'utilisation :</b></p>
 * <pre>{@code
 * FrequencyTable table = new HashFrequencyTable();
 * table.increment("chat");
 * table.increment("chat");
 * table.increment("chien");
 * table.get("chat");   // 2
 * table.get("chien");  // 1
 * table.get("lapin");  // 0 (absent → 0, pas d'exception)
 * table.total();       // 3
 * table.vocabulary();  // {"chat", "chien"}
 * }</pre>
 *
 * @see ca.udem.ift2015.autocompleter.student.HashFrequencyTable
 */
public interface FrequencyTable {

    /**
     * Incrémente de 1 le compte du token donné.
     * Si le token n'a jamais été observé, son compte passe de 0 à 1.
     *
     * @param token le token à comptabiliser (non nul, non vide)
     */
    void increment(String token);

    /**
     * Retourne le nombre d'occurrences du token donné.
     *
     * @param token le token à consulter
     * @return le compte (≥ 0) ; {@code 0} si le token est absent de la table
     */
    int get(String token);

    /**
     * Retourne la somme de tous les comptes (nombre total d'occurrences enregistrées).
     *
     * <p>Équivalent à {@code vocabulary().stream().mapToInt(this::get).sum()}, mais doit
     * être calculé en O(1) dans l'implémentation.</p>
     *
     * @return le total des occurrences (≥ 0)
     */
    int total();

    /**
     * Retourne l'ensemble des tokens ayant été observés au moins une fois.
     *
     * <p>La vue retournée reflète l'état courant de la table ; toute modification
     * ultérieure de la table est visible dans cette vue.</p>
     *
     * @return l'ensemble du vocabulaire (jamais {@code null}, peut être vide)
     */
    Set<String> vocabulary();

    /**
     * Ajoute {@code count} au compteur du token donné.
     * Si le token n'a jamais été observé, son compteur est initialisé à {@code count}.
     *
     * <p>Équivalent à appeler {@link #increment(String)} {@code count} fois,
     * mais en O(1).</p>
     *
     * @param token le token à comptabiliser (non nul, non vide)
     * @param count la valeur à ajouter (≥ 1)
     */
    void incrementBy(String token, int count);

    /**
     * Indique si la table est vide (aucun token enregistré).
     *
     * @return {@code true} si {@link #total()} == 0
     */
    boolean isEmpty();
}
