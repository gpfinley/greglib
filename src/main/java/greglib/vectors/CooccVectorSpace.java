package greglib.vectors;

import greglib.util.NonblockingBufferedReader;

import java.io.*;
import java.util.*;

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

    // very simple serialization

    public void serialize(OutputStream out) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        writer.write(String.valueOf(allWords.size()));
        writer.write("\n");
        for (String word : allWords) {
            writer.write(word);
            writer.write("\n");
        }
        writer.flush();
    }

    public static CooccVectorSpace deserialize(InputStream in) throws IOException {
        CooccVectorSpace space = new CooccVectorSpace();
        NonblockingBufferedReader reader = new NonblockingBufferedReader(in);
        String line = reader.readLine();
        int size = Integer.parseInt(line);
        for (int i=0; i<size; i++) {
            space.addWord(reader.readLine().trim());
        }
        return space;
    }

}
