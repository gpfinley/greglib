package greglib.vectors;

/**
 * A single object to handle a training example and its context words.
 *
 * Created by greg on 5/25/18.
 */
public class ClassificationContext {

    private int category;
    private String[] tokens;

    public ClassificationContext(int category, String[] tokens) {
        this.category = category;
        this.tokens = tokens;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public String[] getTokens() {
        return tokens;
    }

    public void setTokens(String[] tokens) {
        this.tokens = tokens;
    }
}
