package ca.udem.ift2015.autocompleter.student;

import java.util.HashMap;
import java.util.Set;

import ca.udem.ift2015.autocompleter.model.FrequencyTable;

/**
 * Table de fréquences implémentée avec une HashMap.
 *
 * <p>Chaque appel à {@link #increment(String)} augmente le compteur du token
 * d'une unité. Les méthodes {@link #get(String)}, {@link #total()},
 * {@link #vocabulary()} et {@link #isEmpty()} permettent d'interroger la table.
 */
public class HashFrequencyTable implements FrequencyTable {

    // Ici on utilise une HashMap pour associer chaque token(String) à son compteur (Integer).
    private final HashMap<String, Integer> map = new HashMap<>();

    // totalCount est la somme des compteurs de l'ensemble des tokens.
    private int totalCount = 0;

    /**
     *
     * <p>Si le token n'est pas encore présent, son compteur passe à 1.
     * Le compteur global {@code totalCount} est mis à jour à chaque appel.
     */
    @Override
    public void increment(String token) {
        if (map.containsKey(token)) {
            map.put(token, map.get(token) + 1);
        } else {
            map.put(token, 1);
        }
        totalCount++;
    }

    /**
     *
     * <p>Retourne 0 si le token est absent.
     */
    @Override
    public int get(String token) {
        if (map.containsKey(token)) {
            return map.get(token);
        }
        return 0; 
    }

    /**
     *
     * <p>Ce total doit être maintenu en temps constant — ne pas recalculer
     * à chaque appel.
     */
    @Override
    public int total() {
        return totalCount;
    }

    /**
     * Retourner l'ensemble des tokens connus.
     */
    @Override
    public Set<String> vocabulary() {
        return map.keySet();
    }

    /**
     * Retourner {@code true} si aucun token n'a encore été incrémenté.
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Ajouter {@code count} au compteur du token donné.
     *
     * <p>Si le token est absent, l'initialiser à {@code count}.
     * Mettre à jour {@code totalCount} en conséquence.
     */
    @Override
    public void incrementBy(String token, int count) {
        if (map.containsKey(token)) {
            map.put(token, map.get(token) + count);
        } else {
            map.put(token, count);
        }
        totalCount += count ;
    }
}
