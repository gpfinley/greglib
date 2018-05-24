package greglib.vectors;

import greglib.phrases.Phrase;
import greglib.util.Threading;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

/**
 * Represents a collection of terms (including greglib.phrases) and word greglib.embeddings for them
 * Created by gpfinley on 3/14/16.
 */
public class Embeddings implements Iterable<String>, Serializable {

    // For looking up the index based on the word
    private Map<String,Integer> dictionary;
    // For finding the word based on the index
    private List<String> terms;
    private List<WordEmbedding> vectors;

    // Word frequency (from vocab.txt file)
    private List<Integer> frequency;

    private int dimensionality;

    public Embeddings(int dimensionality) {
        this.dimensionality = dimensionality;
        dictionary = new HashMap<>();
        terms = new ArrayList<>();
        vectors = new ArrayList<>();
        frequency = new ArrayList<>();
    }

    public int dimensionality() {
        return dimensionality;
    }

    /**
     * Get the number of occurrences of this word/phrase in the data.
     * SETTING THIS IS OPTIONAL. THIS METHOD WILL RETURN -1 IF NO FREQUENCY HAS BEEN SET.
     * @param phrase the phrase to look up
     * @return the counts of that phrase
     */
    public int getFrequency(String phrase) {
        Integer wordInt = dictionary.get(phrase);
        if(wordInt == null) return 0;
        return frequency.get(dictionary.get(phrase));
    }

    public int getRank(String phrase) {
        return dictionary.get(phrase) + 1;
    }

    public void setWordFrequency(String phrase, int freq) {
        if(!dictionary.containsKey(phrase)) return;
        frequency.set(dictionary.get(phrase), freq);
    }

    public void addWordAndEmbedding(String phrase, WordEmbedding embedding) {
        if(dictionary.containsKey(phrase)) {
            return;
        }
        dictionary.put(phrase, dictionary.size());
        terms.add(phrase);
        vectors.add(embedding);
        frequency.add(-1);
    }

    public WordEmbedding get(String phrase) {
        if(!dictionary.containsKey(phrase)) return null;
        return vectors.get(dictionary.get(phrase));
    }

    /**
     * Remove all greglib.embeddings other than those provided in a set of Strings
     * @param toKeep
     */
    public void filterOn(Set<String> toKeep) {
        for(int i=0; i<size(); i++) {
            if(!toKeep.contains(terms.get(i))) {
                terms.set(i, null);
                vectors.set(i, null);
                dictionary.remove(terms.get(i));
            }
        }
        List<String> newTerms = new ArrayList<>();
        List<WordEmbedding> newVectors = new ArrayList<>();
        Map<String, Integer> newDictionary = new HashMap<>();
        int j=0;
        for(int i=0; i<terms.size(); i++) {
            if(terms.get(i) != null) {
                newDictionary.put(terms.get(i), j);
                newTerms.add(terms.get(i));
                newVectors.add(vectors.get(i));
                j++;
            }
        }
        terms = newTerms;
        dictionary = newDictionary;
        vectors = newVectors;
    }

    /**
     * Get the n most semantically similar greglib.phrases to this one, along with their correlations
     * Assumes an already normalized space for speed
     * Will not exclude any greglib.vectors! That must be done by the caller
     * @param phrase
     * @param n
     * @return
     */
    public Map<String, Double> getTopNSimilar(String phrase, int n) {
        WordEmbedding we = get(phrase);
        return getTopNSimilar(we, n);
    }
    // todo: call threaded!
    public Map<String, Double> getTopNSimilar(WordEmbedding we, int n) {
//        int phraseIndex = dictionary.get(phrase);
        List<String> mostSimilarStrings = new ArrayList<>();
        List<Double> mostSimilarScores = new ArrayList<>();
        Map<String, Double> mostSimilar = new LinkedHashMap<>();
        double[] scores = new double[vectors.size()];
        for (int j = 0; j < scores.length; j++) {
            scores[j] = we.dot(vectors.get(j));
        }
//        scores[phraseIndex] = 0;
        mostSimilarStrings.add(terms.get(0));
        mostSimilarScores.add(scores[0]);
        for (int i = 0; i < scores.length; i++) {
            for (int j = 0; j < n; j++) {
                if (j >= mostSimilarStrings.size()) {
                    mostSimilarScores.add(scores[i]);
                    mostSimilarStrings.add(terms.get(i));
                    break;
                }
                if (scores[i] > mostSimilarScores.get(j)) {
                    mostSimilarScores.add(j, scores[i]);
                    mostSimilarStrings.add(j, terms.get(i));
                    break;
                }
            }
        }
        for (int i = 0; i < n; i++) {
            mostSimilar.put(mostSimilarStrings.get(i), mostSimilarScores.get(i));
        }
        return mostSimilar;
    }

    // todo: call threaded!
    public String mostSimilarTo(WordEmbedding embedding) {
        double best = -Double.MAX_VALUE;
        String bestString = "";
        for(int j=0; j<vectors.size(); j++) {
            double score = embedding.dot(vectors.get(j));
            if(score > best) {
                bestString = terms.get(j);
                best = score;
            }
        }
        return bestString;
    }

    public WordEmbedding getSumVector(Collection<String> sums, @Nullable Collection<String> differences) {
        WordEmbedding ans = new WordEmbedding(dimensionality);
        for(String addword : sums) {
            if(dictionary.containsKey(addword)) {
                int wordInt = dictionary.get(addword);
                ans.add(vectors.get(dictionary.get(addword)));
            }
            else
                System.out.println("WARNING: word " + addword + " not in dictionary; ignoring");
        }
        if(differences != null) {
            for (String subword : differences) {
                if(dictionary.containsKey(subword)) {
                    ans.subtract(vectors.get(dictionary.get(subword)));
                }
                else
                    System.out.println("WARNING: word " + subword + " not in dictionary; ignoring");
            }
        }
        return ans;
    }

    public void normalizeAll() {
        for(WordEmbedding embedding : vectors)
            embedding.normalize();
    }

    public boolean contains(String phrase) {
        return dictionary.containsKey(phrase);
    }

    /**
     * Return true if the semantic space contains all words in this phrase
     * @param phrase
     * @return
     */
    public boolean containsWordsInPhrase(Phrase phrase) {
        for(String word : phrase.getWords()) {
            if(!contains(word)) return false;
        }
        return true;
    }

    public int size() {
        return terms.size();
    }

    @Override
    public Iterator<String> iterator() {
        return terms.iterator();
    }

    /**
     * Fast parallelized calculation of dot products for a whole vocabulary
     * @param thisEmb a word embedding of the same dimensionality as this object
     * @return a double array containing each embedding's dot product with every embedding
     */
    public double[] calculateScoresThreaded(WordEmbedding thisEmb) {
        int n = vectors.size();
        double[] scores = new double[n];
        Threading.chunkAndThread(n, DotThread.class, scores, thisEmb, vectors);
        return scores;
    }

    public static class DotThread extends Threading.IntRangeThread {
        private double[] results;
        private WordEmbedding emb;
        private List<WordEmbedding> vectors;

        @Override
        public void initializeParams(Object[] args) {
            this.results = (double[]) args[0];
            this.emb = (WordEmbedding) args[1];
            this.vectors = (List<WordEmbedding>) args[2];
        }
        @Override
        public void run() {
            for (int i = begin; i < end; i++) {
                results[i] = vectors.get(i).dot(emb);
            }
        }
    }

    public Iterator<WordEmbedding> embeddingIterator() {
        return vectors.iterator();
    }

    /**
     * Slow method--copies the entire list of terms. Better to use the iterator
     * @return
     */
    public List<String> getLexicon() {
        return new ArrayList<>(terms);
    }


    /**
     * Override the default read/write object
     * @param stream
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(dictionary);
        stream.writeObject(terms);
        stream.writeObject(vectors);
        stream.writeObject(frequency);
        stream.writeObject(dimensionality);
    }
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        dictionary = (Map<String, Integer>) stream.readObject();
        terms = (List<String>) stream.readObject();
        vectors = (List<WordEmbedding>) stream.readObject();
        frequency = (List<Integer>) stream.readObject();
        dimensionality = (int) stream.readObject();
    }

}
