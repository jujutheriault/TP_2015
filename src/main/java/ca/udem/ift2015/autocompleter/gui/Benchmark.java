package ca.udem.ift2015.autocompleter.gui;

import ca.udem.ift2015.autocompleter.model.NGramModel;
import ca.udem.ift2015.autocompleter.preprocessing.TextTokenizer;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Mesure les performances du modèle de langage sur un corpus donné.
 *
 * <h2>Métriques</h2>
 * <ol>
 *   <li><b>Temps d'entraînement</b> — durée de {@link NGramModel#train} en ms.</li>
 *   <li><b>Temps de requête moyen topK</b> — moyenne sur {@value #QUERY_ITERATIONS}
 *       appels à {@link NGramModel#topK} en µs.</li>
 *   <li><b>Temps de requête moyen complete</b> — moyenne sur {@value #QUERY_ITERATIONS}
 *       appels à {@link NGramModel#complete} avec préfixes 2-3 chars en µs.</li>
 *   <li><b>Empreinte mémoire</b> — estimation via {@link Runtime} après GC, en MB.</li>
 * </ol>
 *
 * <p>Cette classe est <b>fournie</b> ; les étudiants ne doivent pas la modifier.</p>
 */
public class Benchmark {

    /** Nombre d'appels pour le calcul du temps de requête moyen. */
    public static final int QUERY_ITERATIONS = 1_000;

    /** Valeur de k utilisée pour les requêtes de benchmark. */
    public static final int BENCH_K = 5;

    /**
     * Exécute le benchmark complet sur {@code model} avec le corpus {@code corpusPath}.
     *
     * @param model      le modèle à mesurer (non nul)
     * @param corpusPath chemin vers le fichier texte du corpus
     * @param out        flux de sortie pour les résultats
     * @throws IOException si le fichier est introuvable ou illisible
     */
    public static void run(NGramModel model, String corpusPath, PrintStream out)
            throws IOException {

        out.println("=== Benchmark ===");
        out.println("Corpus : " + corpusPath);

        // ---- 1. Temps d'entraînement ----------------------------------------
        long trainStart = System.currentTimeMillis();
        model.train(new TextTokenizer(new FileReader(corpusPath)).tokenize());
        long trainMs = System.currentTimeMillis() - trainStart;

        out.printf("Temps d'entraînement : %d ms%n", trainMs);
        out.printf("Unigrammes : %d  |  Bigrammes : %d  |  Trigrammes : %d%n",
                model.unigramCount(), model.bigramCount(), model.trigramCount());

        // ---- 2. Mémoire (estimation) ----------------------------------------
        Runtime rt = Runtime.getRuntime();
        rt.gc();
        long usedBytes = rt.totalMemory() - rt.freeMemory();
        out.printf("Mémoire utilisée (estimation) : %.1f MB%n",
                usedBytes / (1024.0 * 1024.0));

        // ---- 3. Temps topK moyen --------------------------------------------
        List<String> sampleTokens = collectSampleTokens(corpusPath, 200);
        if (sampleTokens.isEmpty()) {
            out.println("Temps de requête moyen : (aucun token disponible)");
            return;
        }

        // Préchauffage JIT
        for (int i = 0; i < 50; i++) {
            model.topK(BENCH_K, sampleTokens.get(i % sampleTokens.size()));
        }

        long queryStart = System.nanoTime();
        for (int i = 0; i < QUERY_ITERATIONS; i++) {
            model.topK(BENCH_K, sampleTokens.get(i % sampleTokens.size()));
        }
        long queryNs = System.nanoTime() - queryStart;
        long avgQueryUs = queryNs / (QUERY_ITERATIONS * 1_000L);
        out.printf("Temps de requête moyen (topK, k=%d) : %d µs%n", BENCH_K, avgQueryUs);

        // ---- 4. Temps complete moyen ----------------------------------------
        List<String> prefixes = generatePrefixes(sampleTokens, 200);
        if (!prefixes.isEmpty()) {
            // Préchauffage
            for (int i = 0; i < 50; i++) {
                model.complete(prefixes.get(i % prefixes.size()), BENCH_K);
            }
            long completeStart = System.nanoTime();
            for (int i = 0; i < QUERY_ITERATIONS; i++) {
                model.complete(prefixes.get(i % prefixes.size()), BENCH_K);
            }
            long completeNs = System.nanoTime() - completeStart;
            long avgCompleteUs = completeNs / (QUERY_ITERATIONS * 1_000L);
            out.printf("Temps de requête moyen (complete, k=%d) : %d µs%n", BENCH_K, avgCompleteUs);
        }

        out.println("=================");
    }

    /**
     * Exécute le benchmark sur plusieurs corpus et écrit les résultats au format CSV.
     *
     * <p>Format CSV :</p>
     * <pre>
     * corpus,train_ms,unigrams,bigrams,trigrams,memory_mb,avg_topk_us,avg_complete_us
     * </pre>
     *
     * @param models       un modèle frais par corpus
     * @param corpusPaths  chemins des corpus à tester
     * @param out          flux de sortie CSV
     */
    public static void runSuite(NGramModel[] models, String[] corpusPaths, PrintStream out)
            throws IOException {

        out.println("corpus,train_ms,unigrams,bigrams,trigrams,memory_mb,avg_topk_us,avg_complete_us");

        for (int i = 0; i < corpusPaths.length; i++) {
            String path = corpusPaths[i];
            NGramModel model = models[i];

            // Entraînement
            long trainStart = System.currentTimeMillis();
            model.train(new TextTokenizer(new FileReader(path)).tokenize());
            long trainMs = System.currentTimeMillis() - trainStart;

            // Mémoire
            Runtime rt = Runtime.getRuntime();
            rt.gc();
            double memMb = (rt.totalMemory() - rt.freeMemory()) / (1024.0 * 1024.0);

            // topK
            List<String> sample = collectSampleTokens(path, 100);
            long avgTopKUs = 0;
            if (!sample.isEmpty()) {
                long t0 = System.nanoTime();
                for (int q = 0; q < QUERY_ITERATIONS; q++) {
                    model.topK(BENCH_K, sample.get(q % sample.size()));
                }
                avgTopKUs = (System.nanoTime() - t0) / (QUERY_ITERATIONS * 1_000L);
            }

            // complete
            List<String> prefixes = generatePrefixes(sample, 100);
            long avgCompleteUs = 0;
            if (!prefixes.isEmpty()) {
                long t0 = System.nanoTime();
                for (int q = 0; q < QUERY_ITERATIONS; q++) {
                    model.complete(prefixes.get(q % prefixes.size()), BENCH_K);
                }
                avgCompleteUs = (System.nanoTime() - t0) / (QUERY_ITERATIONS * 1_000L);
            }

            String name = path.replaceAll(".*/", "");
            out.printf("%s,%d,%d,%d,%d,%.1f,%d,%d%n",
                    name, trainMs,
                    model.unigramCount(), model.bigramCount(), model.trigramCount(),
                    memMb, avgTopKUs, avgCompleteUs);
        }
    }

    // -------------------------------------------------------------------------
    // Utilitaires internes
    // -------------------------------------------------------------------------

    /**
     * Lit les premiers {@code maxTokens} tokens du corpus (hors tokens spéciaux).
     */
    private static List<String> collectSampleTokens(String corpusPath, int maxTokens)
            throws IOException {
        List<String> tokens = new ArrayList<>();
        List<List<String>> sentences = new TextTokenizer(new FileReader(corpusPath)).tokenize();
        for (List<String> sentence : sentences) {
            for (String t : sentence) {
                tokens.add(t);
                if (tokens.size() >= maxTokens) return tokens;
            }
        }
        return tokens;
    }

    /**
     * Génère des préfixes de 2-3 caractères à partir des tokens échantillonnés.
     */
    private static List<String> generatePrefixes(List<String> tokens, int max) {
        List<String> prefixes = new ArrayList<>();
        for (String t : tokens) {
            if (t.length() >= 2) {
                int len = t.length() >= 3 ? 3 : 2;
                prefixes.add(t.substring(0, len));
            }
            if (prefixes.size() >= max) break;
        }
        return prefixes;
    }
}
