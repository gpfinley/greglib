package greglib.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Simple utility to read directly from an input stream without blocking it. Useful for deserialization.
 *
 * Created by greg on 5/31/18.
 */
public class NonblockingBufferedReader {

    private InputStream in;

    public NonblockingBufferedReader(InputStream in) {
        this.in = in;
    }

    public String readLine() throws IOException {
        StringBuilder builder = new StringBuilder();
        int next;
        while ((next = in.read()) != -1) {
            if ((char) next == '\n') {
                break;
            }
            builder.append((char) next);
        }
        // If no bytes were read, then a BufferedReader should have returned null
        if (next == -1 && builder.length() == 0) return null;
        return builder.toString();
    }
}
