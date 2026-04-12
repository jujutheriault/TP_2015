package ca.udem.ift2015.autocompleter.student;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import ca.udem.ift2015.autocompleter.model.FrequencyTable;
import ca.udem.ift2015.autocompleter.model.TopKStrategy;

/**
 * Sélection des k tokens les plus fréquents via un min-tas de taille k.
 *
 * <p>Complexité cible : O(n log k) où n = taille du vocabulaire.
 */
public class HeapTopKStrategy implements TopKStrategy {

    /**
     * 
     * Retourner les {@code k} tokens les plus fréquents de {@code table},
     * triés par fréquence décroissante (à égalité : ordre lexicographique croissant).
     *
     * <p>Algorithme attendu :
     * <ol>
     *   <li>Si k ≤ 0 ou la table est vide, retourner une liste vide.</li>
     *   <li>Définir un comparateur qui trie par fréquence croissante, puis par ordre
     *       lexicographique décroissant (le « moins bon » est au sommet du tas).</li>
     *   <li>Parcourir {@code table.vocabulary()} : ajouter chaque token au min-tas ;
     *       si la taille dépasse k, retirer le sommet ({@code heap.poll()}).</li>
     *   <li>Trier le contenu du tas avec le même comparateur, puis inverser →
     *       ordre décroissant final.</li>
     * </ol>
     */
    @Override
    public List<String> topK(FrequencyTable table, int k) {
        // si k est invalide on retourne une liste vide
        if ( k <= 0 ||table.isEmpty()) {
            return new ArrayList<>();
        }

        Comparator<String> comparator = (a,b) -> {
            // on compare les fréquences des 2 tokenss
            int freqCroissance = Integer.compare(table.get(a), table.get(b));

            // si les fréquences sont différentes on retourne le résultat
            if (freqCroissance != 0) {
                return freqCroissance;
            }
            // si non on les compares
            return b.compareTo(a);
        };

        // min heap du comparateur
        PriorityQueue<String> heap = new PriorityQueue<>(comparator);
        // On parcourt les tokens puis on ajoute le nouveau token dans le tas
        for (String token : table.vocabulary()) {
            heap.offer(token);
            // Si la taille du tas est supérieur a k on retire le moins bon élément
            if (heap.size() > k) {
                heap.poll();
            }
        }

        List<String> result = new ArrayList<>(heap);
        result.sort(comparator);
        Collections.reverse(result);

        return result;
    }
}
