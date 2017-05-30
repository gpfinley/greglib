package greglib.vectors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the dictionary for a word co-occurrence vector space
 *
 * Created by gpfinley on 4/14/16.
 */
public class CooccVectorSpace implements Serializable {

    private Map<String, Integer> dictionary;
    private List<String> allWords;

    public CooccVectorSpace() {
        dictionary = new HashMap<>();
        allWords = new ArrayList<>();
    }

    /**
     * Add a word to the dictionary, if necessary, and return its index
     * @param word
     * @return
     */
    public int addWord(String word) {
        if(!dictionary.containsKey(word)) {
            dictionary.put(word, dictionary.size());
            allWords.add(word);
        }
        return dictionary.get(word);
    }

    public Integer getIndex(String word) {
        return dictionary.get(word);
    }

    public boolean hasWord(String word) {
        return dictionary.containsKey(word);
    }

    public String getWord(int index) {
        return allWords.get(index);
    }

    public int size() {
        return allWords.size();
    }

}
