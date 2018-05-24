package greglib.io;

import greglib.vectors.Embeddings;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Scanner;

/**
 * Read in some number of text files and convert into binary files of word greglib.embeddings based on each word in the file
 * Skip words that don't have an embedding associated with them
 *
 * Created by gpfinley on 7/21/16.
 */
public class TextToEmbeddingConverter {

    public static void main(String[] args) throws IOException {
        Embeddings emb = Word2vecReader.readBinFile(args[0]);
        byte[] writeBytes = new byte[4];
        for(int i=1; i<args.length; i++) {
            String infile = args[i];
            String outfile;
            if(infile.toLowerCase().endsWith(".txt")) {
                outfile = infile.substring(0, infile.length()-4);
            } else {
                outfile = infile;
            }
            outfile += "_emb_" + emb.dimensionality() + ".bin";
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(outfile));
            String text = new Scanner(new File(infile)).useDelimiter("\\Z").next();
            text = text.toLowerCase();
            String[] words = text.split("\\W+");

            for(String word : words) {
                System.out.print(word + " ");
                if(!emb.contains(word)) continue;

                for (int j=0; j<emb.dimensionality(); j++) {
                    ByteBuffer.wrap(writeBytes).putFloat((float) emb.get(word).get(j));
                    dos.write(writeBytes);
                }
                dos.flush();
            }
            dos.close();
        }
    }
}
