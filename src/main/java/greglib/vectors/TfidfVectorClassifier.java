package greglib.vectors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Build tf-idf vectors from a corpus all at once,
 * then continue to use them for classification.
 * TODO: test
 *
 * Created by greg on 5/25/18.
 */
public class TfidfVectorClassifier {

    private static final Logger LOGGER = Logger.getLogger(TfidfVectorClassifier.class.getName());

    private List<CooccVector> vectors = new ArrayList<>();
    private CooccVectorSpace space = new CooccVectorSpace();
    CooccVector idfVector;

    // used only for deserialization
    private TfidfVectorClassifier() {}

    public TfidfVectorClassifier(Iterable<ClassificationContext> classificationContexts) {
        idfVector = new CooccVector(space);
        for (ClassificationContext cc : classificationContexts) {
           for (String token : cc.getTokens()) {
               while (vectors.size() <= cc.getCategory()) {
                   vectors.add(new CooccVector(space));
               }
               vectors.get(cc.getCategory()).incrementAndAdd(token, 1.);
               idfVector.increment(token, 1.);
           }
        }
        idfVector.applyElementwise(x -> Math.pow(x, -1));
        vectors.forEach(v -> v.hadamard(idfVector));
    }

    public double getScore(int category, CooccVector vector) {
        return vector.cosSim(vectors.get(category));
    }

    public double getProbability(int category, CooccVector vector) {
        if (vector.mag() == 0) return 1./vectors.size();
        List<Double> scores = vectors.stream()
                .map(vector::cosSim)
                .map(s -> Double.isNaN(s) ? 0 : s)
                .collect(Collectors.toList());
        double sum = scores.stream().mapToDouble(d -> d).sum();
        if (sum == 0) return 1./vectors.size();
        return scores.get(category) / sum;
    }

    public CooccVector getTfidfVector(String[] contextTokens, double[] weights) {
        CooccVector vector = new CooccVector(space);
        for (int i=0; i<contextTokens.length; i++) {
            vector.increment(contextTokens[i], weights[i]);
        }
        vector.hadamard(idfVector);
        return vector;
    }

    public CooccVector getTfidfVector(String[] contextTokens) {
        CooccVector vector = new CooccVector(space);
        for (String tok : contextTokens) {
            vector.increment(tok, 1.);
        }
        vector.hadamard(idfVector);
        return vector;
    }

    public void serialize(OutputStream out) throws IOException {
        space.serialize(out);
        idfVector.serialize(out);
        byte[] nCategories = ByteBuffer.allocate(4).putInt(vectors.size()).array();
        out.write(nCategories);
        for (CooccVector vector : vectors) {
            vector.serialize(out);
        }
    }

    public static TfidfVectorClassifier deserialize(InputStream in) throws IOException {
        LOGGER.info("Deserializing TF-IDF vector classifier from stream " + in);
        TfidfVectorClassifier tvc = new TfidfVectorClassifier();
        tvc.space = CooccVectorSpace.deserialize(in);
        tvc.idfVector = CooccVector.deserialize(in, tvc.space);
        tvc.vectors = new ArrayList<>();
        byte[] nCatBytes = new byte[4];
        in.read(nCatBytes);
        int nCategories = ByteBuffer.wrap(nCatBytes).getInt();
        for (int i=0; i<nCategories; i++) {
            tvc.vectors.add(CooccVector.deserialize(in, tvc.space));
        }
        LOGGER.info("Done deserializing TF-IDF vector classifier.");
        return tvc;
    }

}
