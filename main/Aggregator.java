package main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class Aggregator {
    public static CyclicBarrier barrier;

    public static ConcurrentHashMap<String, Set<String>> categories = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Set<String>> languages = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, AtomicInteger> words = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, AtomicInteger> authors = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Article> readArticles = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> seenTitles = new ConcurrentHashMap<>();

    public static Set<String> forbiddenWords = new HashSet<>();
    public static Set<String> duplicatedUuids = ConcurrentHashMap.newKeySet();

    public static AtomicInteger totalCounter = new AtomicInteger(0);
    public static AtomicBoolean CleaningConfirmation = new AtomicBoolean(false);
    public static AtomicBoolean ReportsConfirmation = new AtomicBoolean(false);

    public static LinkedBlockingQueue<String> tasksQueue = new LinkedBlockingQueue<>();
    public static LinkedBlockingQueue<String> finalTasksQueue = new LinkedBlockingQueue<>();

    public static List<Article> activeArticlesList;
    public static AtomicInteger processingIndex = new AtomicInteger(0);

    public static void readInputFiles(String articlesFile, String auxFile) throws IOException, InterruptedException {
        
        Path path = Path.of(articlesFile);
        Path folder = path.getParent();
        List<String> allLines = Files.readAllLines(path);

        if (!allLines.isEmpty()) {
            for (int i = 1; i < allLines.size(); i++) {
                String line = allLines.get(i);
                String filePath = line.trim();
                tasksQueue.put(filePath);
            }
        }

        path = Path.of(auxFile);
        allLines = Files.readAllLines(path);

        if (!allLines.isEmpty()) {

            String line = allLines.get(1);
            String shortFilePath = line.trim();
            String filePath = folder + "/" + shortFilePath;
            Path newPath = Path.of(filePath);
            List<String> newLines = Files.readAllLines(newPath);

            for (int j = 1; j < newLines.size(); j++) {
                languages.put(newLines.get(j), ConcurrentHashMap.newKeySet());
            }

            line = allLines.get(2);
            shortFilePath = line.trim();
            filePath = folder + "/" + shortFilePath;
            newPath = Path.of(filePath);
            newLines = Files.readAllLines(newPath);

            for (int j = 1; j < newLines.size(); j++) {
                categories.put(newLines.get(j), ConcurrentHashMap.newKeySet());
            }

            line = allLines.get(3);
            shortFilePath = line.trim();
            filePath = folder + "/" + shortFilePath;
            newPath = Path.of(filePath);
            newLines = Files.readAllLines(newPath);

            for (int j = 1; j < newLines.size(); j++) {
                forbiddenWords.add(newLines.get(j).trim());
            }
        }

        finalTasksQueue.add("WC");
        finalTasksQueue.add("WL");
        finalTasksQueue.add("WA");
        finalTasksQueue.add("WK");
        finalTasksQueue.add("SA");
    }

    public static void main(String[] args) throws InterruptedException, IOException {

        int numThreads = Integer.parseInt(args[0]);
        String articlesFile = args[1];
        String auxFile = args[2];

        barrier = new CyclicBarrier(numThreads);
        Worker[] workers = new Worker[numThreads];
        Path folder = Path.of(articlesFile).getParent();

        readInputFiles(articlesFile, auxFile);

        for(int i = 0; i < numThreads; i++) {
            workers[i] = new Worker(tasksQueue, readArticles, folder, seenTitles,
                     CleaningConfirmation, duplicatedUuids, ReportsConfirmation, activeArticlesList, processingIndex);
            workers[i].start();
        }

        for(int i = 0; i < numThreads; i++) {
            try {
                workers[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}