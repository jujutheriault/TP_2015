package ca.udem.ift2015.autocompleter.student;

import ca.udem.ift2015.autocompleter.model.FrequencyTable;
import ca.udem.ift2015.autocompleter.model.NGramModel;
import ca.udem.ift2015.autocompleter.model.TopKStrategy;
import ca.udem.ift2015.autocompleter.model.Trie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modèle de langage trigramme avec repli de Katz (Katz Backoff).
 *
 * <p>Structures de données internes :
 * <ul>
 *   <li>{@code unigrams} — fréquence de chaque mot du corpus</li>
 *   <li>{@code bigrams}  — clé = mot précédent, valeur = table de fréquences du mot suivant</li>
 *   <li>{@code trigrams} — clé = {@code "w1 w2"}, valeur = table de fréquences du mot suivant</li>
 *   <li>{@code trie}     — trie préfixe pour complétion de mots</li>
 * </ul>
 */
public class KatzBackoffModel implements NGramModel {

    private final FrequencyTable unigrams;
    private final Map<String, FrequencyTable> bigrams;
    private final Map<String, FrequencyTable> trigrams;
    private final Trie trie;
    private final TopKStrategy strategy;

    public KatzBackoffModel(TopKStrategy strategy) {
        this.strategy = strategy;
        this.unigrams = new HashFrequencyTable();
        this.bigrams  = new HashMap<>();
        this.trigrams = new HashMap<>();
        this.trie     = new PrefixTrie();
    }

    /**
     * TODO 11 — Entraîner le modèle sur une liste de phrases tokenisées.
     *
     * <p>Pour chaque phrase et chaque position {@code i} :
     * <ul>
     *   <li>Incrémenter {@code unigrams} pour {@code sentence.get(i)}.</li>
     *   <li>Insérer le mot dans le {@code trie} via {@code trie.insert(w)}.</li>
     *   <li>Si {@code i >= 1} : incrémenter {@code bigrams[w_{i-1}]} pour {@code w_i}.</li>
     *   <li>Si {@code i >= 2} : incrémenter {@code trigrams["w_{i-2} w_{i-1}"]} pour {@code w_i}.</li>
     * </ul>
     */
    @Override
    public void train(List<List<String>> sentences) {
        throw new UnsupportedOperationException("TODO 11 — train non implémenté");
    }

    /**
     * TODO 12 — Retourner les k mots les plus probables étant donné le contexte.
     *
     * <p><b>Repli strict de Katz (pas de mélange inter-niveaux) :</b> dès qu'un niveau
     * possède des données pour le contexte donné, on l'utilise exclusivement.
     * <ol>
     *   <li>Si {@code context.length >= 2} et que le trigramme {@code "w_{n-2} w_{n-1}"}
     *       est connu dans {@code trigrams} → retourner
     *       {@code strategy.topK(trigrams.get(key), k)}.</li>
     *   <li>Sinon, si {@code context.length >= 1} et que le bigramme {@code w_{n-1}}
     *       est connu dans {@code bigrams} → retourner
     *       {@code strategy.topK(bigrams.get(prev), k)}.</li>
     *   <li>Sinon → retourner {@code strategy.topK(unigrams, k)}.</li>
     * </ol>
     *
     * <p>Si {@code k <= 0} ou que le modèle est vide ({@code unigrams.isEmpty()}),
     * retourner une liste vide.
     */
    @Override
    public List<String> topK(int k, String... context) {
        throw new UnsupportedOperationException("TODO 12 — topK non implémenté");
    }

    /**
     * TODO 13 — Prédire le mot suivant le plus probable.
     *
     * <p>Déléguer à {@code topK(1, context)} et retourner le premier élément,
     * ou {@code null} si la liste est vide.
     */
    @Override
    public String predict(String... context) {
        throw new UnsupportedOperationException("TODO 13 — predict non implémenté");
    }

    /**
     * TODO 14 — Retourner les k mots du trie commençant par {@code prefix}.
     *
     * <p>Déléguer à {@code trie.complete(prefix, k)}.
     */
    @Override
    public List<String> complete(String prefix, int k) {
        throw new UnsupportedOperationException("TODO 14 — complete non implémenté");
    }

    /** Fourni — délègue à unigrams.get() (fonctionnel dès que TODO 2 est implémenté). */
    @Override
    public int frequency(String word) {
        return unigrams.get(word);
    }

    @Override
    public int unigramCount() {
        return unigrams.vocabulary().size();
    }

    @Override
    public int bigramCount() {
        return bigrams.values().stream()
                .mapToInt(t -> t.vocabulary().size())
                .sum();
    }

    @Override
    public int trigramCount() {
        return trigrams.values().stream()
                .mapToInt(t -> t.vocabulary().size())
                .sum();
    }
}
