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
            // Si le token est déjà présent on augmente son compteur
            map.put(token, map.get(token) + 1);
        } else {
            // Si le token est nouveau on l'ajoute avec un compteur
            map.put(token, 1);

        }
        // MAJ du totalCount
        totalCount++;
    }

    /**
     *
     * <p>Retourne 0 si le token est absent.
     */
    @Override
    public int get(String token) {
        // Si le tokn existe on va retourner sa fréquence
        if (map.containsKey(token)) {
            return map.get(token);
        }
        // si non sa fréquence est de 0
        return 0; 
    }

    /**
     *
     * <p>Ce total doit être maintenu en temps constant — ne pas recalculer
     * à chaque appel.
     */
    @Override
    public int total() {
        //retour direct, complexité O(1)
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
        // si le token existe
        if (map.containsKey(token)) {
            // alors on ajoute son count à la valeur actuelle
            map.put(token, map.get(token) + count);
        } else {
            // si non on initialise un nouveau token
            map.put(token, count);
        }
        // MAJ de total count (maintient O(1))
        totalCount += count ;
    }
}
