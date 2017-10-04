package greglib.util;

import greglib.phrases.Phrase;

import java.util.*;

/**
 * // todo: describe algorithm and usage
 * Space-efficient collocation storage and lookup.
 * Created by gpfinley on 10/11/16.
 */
public class PhraseGraph {

    private final Map<String, Object> graph;

    public PhraseGraph() {
        graph = new HashMap<>();
    }

    public PhraseGraph(Iterable<Phrase> phrases) {
        graph = new HashMap<>();
        for (Phrase phrase : phrases) {
            addPhrase(phrase);
        }
    }

    public boolean addPhrase(Phrase phrase) {
        List<String> words = new ArrayList<>(phrase.getWords());
        Map<String, Object> addToThisMap = graph;
        while (true) {
            String firstWord = words.get(0);
            addToThisMap.putIfAbsent(firstWord, new HashMap<String, Map>());
            addToThisMap = (Map) addToThisMap.get(firstWord);
            words.remove(0);
            if (words.size() == 0) {
                if (addToThisMap.containsKey(null)) return false;
                addToThisMap.put(null, phrase);
                return true;
            }
        }
    }

    public boolean removePhrase(Phrase phrase) {
        Map<String, Object> whereInGraph = graph;
        try {
            for (String word : phrase.getWords()) {
                whereInGraph = (Map) whereInGraph.get(word);
            }
            whereInGraph.remove(null);
            return true;
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     *
     * @param words a list of tokens
     * @param index the index to start looking in that list
     * @return the longest possible phrase, or null if none found from this index
     */
    public Phrase getLongestPhraseFrom(List<String> words, int index) {
        Phrase lastEligiblePhrase = null;
        Map<String, Object> lookup = graph;
        int i;
        for (i = index; i <= words.size(); i++) {
            lastEligiblePhrase = (Phrase) lookup.getOrDefault(null, lastEligiblePhrase);
            if (i < words.size()) {
                String thisWord = words.get(i);
                if (lookup.containsKey(thisWord)) {
                    lookup = (Map) lookup.get(words.get(i));
                } else {
                    break;
                }
            }
        }
        return lastEligiblePhrase;
    }

    public static void main(String[] args) {
        Phrase[] phrases = new Phrase[]{
            new Phrase("one"),
            new Phrase("two"),
                new Phrase("two three"),
            new Phrase("three")
        };
        PhraseGraph pg = new PhraseGraph(Arrays.asList(phrases));
        System.out.println(pg.graph);
        System.out.println(pg.getLongestPhraseFrom("one two three".split(" "), 0));
        System.out.println(pg.getLongestPhraseFrom("one two three".split(" "), 1));
        System.out.println(pg.getLongestPhraseFrom("one two three".split(" "), 2));
    }

    // todo: is this as quick as just copying the above code for arrays?
    public Phrase getLongestPhraseFrom(String[] words, int index) {
        return getLongestPhraseFrom(Arrays.asList(words), index);
    }

    @Override
    public String toString() {
        return graph.toString();
    }

}
