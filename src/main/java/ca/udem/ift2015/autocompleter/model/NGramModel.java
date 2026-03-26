package ca.udem.ift2015.autocompleter.model;

import java.util.List;

/**
 * Modèle de langage à n-grammes avec backoff de Katz et complétion par préfixe.
 *
 * <p>Un {@code NGramModel} apprend des statistiques de co-occurrence à partir d'un
 * corpus de phrases tokenisées, puis prédit les tokens les plus probables selon
 * un contexte de 0 à 2 mots (trigramme → bigramme → unigramme).</p>
 *
 * <h2>Cycle de vie</h2>
 * <ol>
 *   <li><b>Entraînement</b> — appeler {@link #train(List)} avec la liste de phrases
 *       issue de {@link ca.udem.ift2015.autocompleter.preprocessing.TextTokenizer#tokenize()}.
 *       Peut être appelé plusieurs fois pour enrichir le modèle.</li>
 *   <li><b>Prédiction</b> — appeler {@link #predict(String...)} ou
 *       {@link #topK(int, String...)} pour obtenir des suggestions.</li>
 *   <li><b>Complétion</b> — appeler {@link #complete(String, int)} pour obtenir
 *       les mots du vocabulaire dont le préfixe correspond.</li>
 * </ol>
 *
 * <h2>Règles de prédiction (Katz Backoff)</h2>
 * <ul>
 *   <li>Si le contexte contient ≥ 2 mots et que des trigrammes sont connus
 *       pour ces 2 mots → utiliser la table de trigrammes.</li>
 *   <li>Sinon, si le contexte contient ≥ 1 mot et que des bigrammes sont connus
 *       → utiliser la table de bigrammes.</li>
 *   <li>Sinon → se replier sur les unigrammes.</li>
 *   <li>À fréquence égale, ordre lexicographique croissant.</li>
 * </ul>
 *
 * <p><b>Exemple de session :</b></p>
 * <pre>{@code
 * NGramModel model = new KatzBackoffModel(new HeapTopKStrategy());
 * model.train(new TextTokenizer(new FileReader("corpus.txt")).tokenize());
 * System.out.println(model.predict("the", "old"));   // "man"
 * System.out.println(model.topK(3, "the", "old"));   // ["man", "woman", "house"]
 * System.out.println(model.complete("th", 5));        // ["the", "they", "their", ...]
 * System.out.println(model.unigramCount());           // ex. 1247
 * System.out.println(model.bigramCount());            // ex. 3891
 * System.out.println(model.trigramCount());           // ex. 8042
 * }</pre>
 *
 * @see ca.udem.ift2015.autocompleter.student.KatzBackoffModel
 */
public interface NGramModel {

    /**
     * Entraîne le modèle sur une liste de phrases tokenisées.
     *
     * <p>Met à jour les tables d'unigrammes, bigrammes et trigrammes ainsi que
     * le trie de préfixes. Les appels successifs s'accumulent.</p>
     *
     * <p><b>Complexité attendue :</b> O(n) où n est le nombre total de tokens.</p>
     *
     * @param sentences liste de phrases (non nulle) ; chaque phrase est une liste
     *                  de tokens non vides en minuscules
     */
    void train(List<List<String>> sentences);

    /**
     * Prédit le token le plus probable selon le contexte donné (backoff de Katz).
     *
     * <p>Équivalent à {@code topK(1, context).isEmpty() ? null : topK(1, context).get(0)}.</p>
     *
     * @param context 0, 1 ou 2 tokens de contexte (varargs)
     * @return le token prédit, ou {@code null} si le modèle n'a jamais été entraîné
     */
    String predict(String... context);

    /**
     * Retourne les {@code k} tokens les plus probables selon le contexte (backoff de Katz).
     *
     * <p>Le contexte peut contenir 0, 1 ou 2 tokens :</p>
     * <ul>
     *   <li>2 tokens → cherche d'abord dans les trigrammes, puis backoff.</li>
     *   <li>1 token  → cherche d'abord dans les bigrammes, puis backoff.</li>
     *   <li>0 token  → utilise directement les unigrammes.</li>
     * </ul>
     *
     * <p><b>Note :</b> {@code k} est le premier paramètre pour éviter l'ambiguïté
     * avec le varargs.</p>
     *
     * <p><b>Complexité attendue :</b> O(n log k) délégué à la stratégie.</p>
     *
     * @param k       nombre maximum de résultats (≥ 0)
     * @param context 0, 1 ou 2 tokens de contexte (varargs)
     * @return liste de tokens, jamais {@code null}, peut être vide
     */
    List<String> topK(int k, String... context);

    /**
     * Retourne les {@code k} mots du vocabulaire dont la représentation commence
     * par {@code prefix}, triés par fréquence décroissante.
     *
     * <p>Si {@code prefix} est vide, retourne les {@code k} unigrammes les plus fréquents.</p>
     *
     * @param prefix le préfixe à compléter (non nul)
     * @param k      nombre maximum de résultats (≥ 0)
     * @return liste de mots triée par fréquence décroissante
     */
    List<String> complete(String prefix, int k);

    /**
     * Retourne la fréquence d'apparition du mot dans le corpus (unigramme).
     *
     * @param word le mot à chercher
     * @return nombre d'occurrences, ou 0 si inconnu
     */
    int frequency(String word);

    /**
     * Retourne le nombre de types distincts dans la table des unigrammes.
     *
     * @return taille du vocabulaire (0 si jamais entraîné)
     */
    int unigramCount();

    /**
     * Retourne le nombre de bigrammes distincts observés.
     *
     * @return nombre de paires (u, v) distinctes (0 si jamais entraîné)
     */
    int bigramCount();

    /**
     * Retourne le nombre de trigrammes distincts observés.
     *
     * @return nombre de triplets (u, v, w) distincts (0 si jamais entraîné)
     */
    int trigramCount();
}
