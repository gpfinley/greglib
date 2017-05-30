package greglib.vectors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Simple class for handling sparse word co-occurrence greglib.vectors (using Double for values to deal with weighting, etc.)
 *
 * Created by gpfinley on 4/14/16.
 */
public class CooccVector implements Serializable {

    private Map<Integer, Double> vector;

    public final CooccVectorSpace space;

    public CooccVector(CooccVectorSpace space) {
        vector = new HashMap<>();
        this.space = space;
    }

    public CooccVector(CooccVector orig) {
        this.space = orig.space;
        vector = new HashMap<>();
        for(Map.Entry<Integer, Double> e : orig.vector.entrySet()) {
            vector.put(e.getKey(), e.getValue());
        }
    }

    public double get(int i) {
        return vector.getOrDefault(i, 0.0);
    }

    public int size() {
        return space.size();
    }

    // ERASE THIS FUNCTION IF DESERIALIZING VECTORS FROM DEATHSTAR
    /**
     * Increment the count for this word (and update the dictionary in the associated space if necessary)
     */
    public void incrementAndAdd(String word, double weight) {
        space.addWord(word);
        increment(word, weight);
    }

    /**
     * Increment the count for this word, but don't add the word to the dictionary
     * @param word
     * @param weight
     */
    public void increment(String word, double weight) {
        Integer index = space.getIndex(word);
        if(index == null) return;
        if(vector.containsKey(index)) {
            vector.put(index, vector.get(index) + weight);
        } else {
            vector.put(index, weight);
        }
    }

    public Set<Integer> getNonZeros() {
        return vector.keySet();
    }

    public void normalize() {
        double mag = mag();
        for(int k : vector.keySet()) {
            vector.put(k, vector.get(k)/mag);
        }
    }

    public double mag() {
        double sqsum = 0;
        for(double x : vector.values()) {
            sqsum += x * x;
        }
        return Math.sqrt(sqsum);
    }

    public double dot(CooccVector other) {
        double sum = 0;
        for(int k : vector.keySet()) {
            sum += vector.get(k) * other.get(k);
        }
        return sum;
    }

    public CooccVector sum(CooccVector other) {
        CooccVector sumVec = new CooccVector(this);
        sumVec.add(other);
        return sumVec;
    }

    public CooccVector difference(CooccVector other) {
        CooccVector diffVec = new CooccVector(this);
        for(int k : other.vector.keySet()) {
            if(vector.get(k) == null) {
                vector.put(k, other.get(k));
            } else {
                vector.put(k, other.get(k) - vector.get(k));
            }
        }
        return diffVec;
    }

    public void add(CooccVector other) {
        for(int k : other.vector.keySet()) {
            if(vector.get(k) == null) {
                vector.put(k, other.get(k));
            } else {
                vector.put(k, other.get(k) + vector.get(k));
            }
        }
    }

    public void scalarMultiply(double s) {
        for(int k : vector.keySet()) {
            vector.put(k, vector.get(k) * s);
        }
    }

    public double cosSim(CooccVector other) {
        return dot(other) / mag() / other.mag();
    }

    @Override
    public String toString() {
        return vector.toString();
    }
}
