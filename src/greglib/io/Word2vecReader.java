package greglib.io;

import greglib.vectors.Embeddings;
import greglib.vectors.WordEmbedding;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Reads a greglib.vectors file as output by the word2vec C program into a WordVectorSpace
 * Created by gpfinley on 2/9/16.
 */
public class Word2vecReader {

    private static Logger LOGGER = Logger.getLogger(Word2vecReader.class.getName());

    public static Embeddings readBinFile(String filename) throws IOException {
        return readBinFile(filename, 0);
    }

    public static Embeddings readBinFile(String filename, int maxWords) throws IOException {
        LOGGER.info("Reading greglib.vectors from word2vec binary file...");
        InputStream reader = new FileInputStream(filename);
        char c;
        String nWordsStr = "";
        while(true) {
            c = (char)reader.read();
            if(c==' ') break;
            nWordsStr += c;
        }
        String sizeStr = "";
        while(true) {
            c = (char)reader.read();
            if(c=='\n') break;
            sizeStr += c;
        }
        int nWords = Integer.parseInt(nWordsStr);
        if (maxWords > 0 && maxWords < nWords) {
            nWords = maxWords;
        }
        int size = Integer.parseInt(sizeStr);
        Embeddings wes = new Embeddings(size);
        char firstchar = '\n';
        byte[] bytes = new byte[size*4];
        for(int i=0; i<nWords; i++) {
            String word = "";
            if(firstchar != '\n')
                word += firstchar;
            while((c = (char)reader.read()) != ' ') {
                word += c;
            }
            // Read in all bytes associated with this vector
            reader.read(bytes, 0, size*4);
            float vector[] = new float[size];
            for(int j=0; j<size; j++) {
                vector[j] = ByteBuffer.wrap(bytes, j*4, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            }
            // For some files, there's an extra \n (such as those generated by the C word2vec)
            // For others, there's no newline--it goes straight to the next word (the GoogleNews greglib.vectors, e.g.)
            firstchar = (char) reader.read();
            if (word.length() > 0) {
                wes.addWordAndEmbedding(word, new WordEmbedding(vector));
            }
            else {
                LOGGER.info("Not including zero-length phrase in Embeddings (don't worry about it)");
            }
        }
        LOGGER.info("Read " + nWords + " word greglib.vectors with " + sizeStr + " dimensions");

        return wes;
    }

    /**
     * Read all words that have at least a certain number of appearances in the corpus
     * @param binFile the binary file of greglib.embeddings
     * @param vocabFile the vocabulary file with occurrence stats
     * @param minFreq the minimum number of occurrences of a word
     * @return an Embeddings object with only words of high enough frequency
     * @throws IOException
     */
    public static Embeddings readBinFile(String binFile, String vocabFile, int minFreq) throws IOException {
        BufferedReader vocabReader = new BufferedReader(new FileReader(vocabFile));
        String line;
        int howManyWords = 0;
        while((line = vocabReader.readLine()) != null) {
            String[] fields = line.split("\\s+");
            if(Integer.parseInt(fields[1]) < minFreq) {
                return readBinFile(binFile, howManyWords);
            }
            howManyWords++;
        }
        LOGGER.warning("No words less than specified minimum frequency; reading all words");
        return readBinFile(binFile, 0);
    }

    /**
     * Read in a word2vec vocab file to get frequency counts of all words and greglib.phrases.
     * @param vocabFile a file path
     * @return a map between greglib.phrases and their counts
     * @throws IOException
     */
    public static Map<String, Integer> readVocabFile(String vocabFile, int nWords) throws IOException {
        Map<String, Integer> counts = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(vocabFile));
        String line;
        Pattern splitter = Pattern.compile("\\s+");
        while((line = reader.readLine()) != null) {
            String[] fields = splitter.split(line);
            counts.put(fields[0], Integer.parseInt(fields[1]));
            if (counts.size() >= nWords && nWords > 0) break;
        }
        return counts;
    }
    public static Map<String, Integer> readVocabFile(String vocabFile) throws IOException {
        return readVocabFile(vocabFile, 0);
    }

    public static Embeddings readBinAndVocab(String binFile, String vocabFile, int nWords) throws IOException {
        Embeddings emb = readBinFile(binFile, nWords);
        Map<String, Integer> counts = readVocabFile(vocabFile);
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            emb.setWordFrequency(e.getKey(), e.getValue());
        }
        return emb;
    }

    public static Embeddings readBinAndVocab(String binFile, String vocabFile) throws IOException {
        return readBinAndVocab(binFile, vocabFile, 0);
    }

}