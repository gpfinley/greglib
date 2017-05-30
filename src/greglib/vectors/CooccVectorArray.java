package greglib.vectors;

import java.io.Serializable;
import java.util.Arrays;

/**
 * non-sparse word co-occurrence greglib.vectors. Need a vector space pre-defined.
 *
 * Created by gpfinley on 4/14/16.
 */
public class CooccVectorArray implements Serializable {

    private float[] vector;

    public final CooccVectorSpace space;

    public CooccVectorArray(CooccVectorSpace space) {
        this.space = space;
        vector = new float[space.size()];
    }

    public CooccVectorArray(CooccVectorArray orig) {
        this.space = orig.space;
        vector = Arrays.copyOf(orig.vector, orig.vector.length);
    }

    public double get(int i) {
        return vector[i];
    }

    public int size() {
        return vector.length;
    }

    /**
     * Increment the count for this word (can't add the word to the dictionary)
     * @param word
     * @param weight
     */
    public void increment(String word, double weight) {
        Integer index = space.getIndex(word);
        if(index == null) return;
        vector[index] += weight;
    }

    public void normalize() {
        double mag = mag();
        if(mag == 0) return;
        for(int i=0; i<vector.length; i++) {
            vector[i] /= mag;
        }
    }

    public double mag() {
        double sqsum = 0;
        for(float x : vector) {
            sqsum += x * x;
        }
        return Math.sqrt(sqsum);
    }

    public double dot(CooccVectorArray other) {
        double sum = 0;
        for(int i=0; i<vector.length; i++) {
            sum += vector[i] * other.get(i);
        }
        return sum;
    }

    public CooccVectorArray sum(CooccVectorArray other) {
        CooccVectorArray sumVec = new CooccVectorArray(this);
        sumVec.add(other);
        return sumVec;
    }

    public CooccVectorArray difference(CooccVectorArray other) {
        CooccVectorArray diffVec = new CooccVectorArray(this);
        diffVec.subtract(other);
        return diffVec;
    }

    public void add(CooccVectorArray other) {
        for(int i=0; i<vector.length; i++) {
            vector[i] += other.get(i);
        }
    }

    public void subtract(CooccVectorArray other) {
        for (int i = 0; i < vector.length; i++) {
            vector[i] -= other.get(i);
        }
    }

    public void scalarMultiply(double s) {
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= s;
        }
    }

    public double cosSim(CooccVectorArray other) {
        return dot(other) / mag() / other.mag();
    }

    @Override
    public String toString() {
        return Arrays.toString(vector);
    }

}
