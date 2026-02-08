package main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Helpers {
    public static int compareArticles(Article a1, Article a2) {
        int cmp = a2.getPublished().compareTo(a1.getPublished());

        if(cmp != 0) {
            return cmp;
        }

        return a1.getUuid().compareTo(a2.getUuid());
    }

    public static int compareMapEntries(Map.Entry<String, Set<String>> a1, Map.Entry<String, Set<String>> a2) {
        if(a2.getValue().size() < a1.getValue().size()) {
            return -1;
        } else if (a2.getValue().size() > a1.getValue().size()){
            return 1;
        }

        return a1.getKey().compareTo(a2.getKey());
    }

    public static int sortWordsAndAuthors(Map.Entry<String, AtomicInteger> a1, Map.Entry<String, AtomicInteger> a2) {
        if(a2.getValue().get() < a1.getValue().get()) {
            return -1;
        } else if (a2.getValue().get() > a1.getValue().get()){
            return 1;
        }

        return a1.getKey().compareTo(a2.getKey());
    }

    public static void writeToFile(String fileName, List<String> lines) {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
