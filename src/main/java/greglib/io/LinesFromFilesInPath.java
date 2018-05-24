package greglib.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Iterate over lines from all files in this path recursively
 * Created by gpfinley on 6/23/16.
 */
public class LinesFromFilesInPath implements Iterator<String> {

    Queue<File> fileQueue;
    BufferedReader reader;
    String nextLine;

    public LinesFromFilesInPath(Path path) throws IOException {
        fileQueue = new LinkedList<>();
        addFilesFromDir(path.toFile());

        System.out.println(fileQueue);

        loadNextFile();
    }

    private void addFilesFromDir(File dir) {
        File[] files = dir.listFiles();
        for(File file : files) {
            if(file.getName().startsWith(".")) continue;
            if(file.isDirectory()) {
                addFilesFromDir(file);
            } else {
                fileQueue.add(file);
            }
        }
    }

    private void loadNextFile() throws IOException {
        reader = new BufferedReader(new FileReader(fileQueue.remove()));
        nextLine = reader.readLine();
    }

    @Override
    public String next() throws NoSuchElementException {
        String thisLine = nextLine;
        try {
            nextLine = reader.readLine();
            if (nextLine == null && !fileQueue.isEmpty()) {
                loadNextFile();
            }
        } catch(IOException e) {
            System.out.println("Couldn't read from file");
            throw new NoSuchElementException();
        }
        return thisLine;
    }

    @Override
    public boolean hasNext() {
        return nextLine != null;
    }
}
