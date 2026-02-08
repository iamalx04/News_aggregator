package main;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class Worker extends Thread{

    private final LinkedBlockingQueue<String> tasksQueue;

    private final ConcurrentHashMap<String, Article> readArticles;
    private final ConcurrentHashMap<String, String> seenTitles;

    private final Set<String> duplicatedUuids;

    private final AtomicBoolean CleaningConfirmation;
    private final AtomicBoolean ReportsConfirmation;

    private final Path root;
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Pattern NON_ALPHA = Pattern.compile("[^a-z]");
    private static final Pattern SPACE = Pattern.compile("\\s+");

    private static List<Article> activeArticlesList;
    private static AtomicInteger processingIndex;

    public Worker(LinkedBlockingQueue<String> tasksQueue, ConcurrentHashMap<String, Article> readArticles, Path root,
                  ConcurrentHashMap<String, String> seenTitles, AtomicBoolean CleaningConfirmation,
                  Set<String> duplicatedUuids, AtomicBoolean ReportsConfirmation, List<Article> activeArticlesList,
                  AtomicInteger processingIndex){
        this.tasksQueue = tasksQueue;
        this.readArticles = readArticles;
        this.root = root;
        this.seenTitles = seenTitles;
        this.CleaningConfirmation = CleaningConfirmation;
        this.duplicatedUuids = duplicatedUuids;
        this.ReportsConfirmation = ReportsConfirmation;
        this.activeArticlesList = activeArticlesList;
        this.processingIndex = processingIndex;
    }

    public void runArticlesReading(String shortPath) {
        List<Article> articles;
        String path = root + "/" + shortPath;

        try {
            articles = mapper.readValue(
                    new File(path),
                    new TypeReference<>() {});

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Article article : articles) {
            Tema1.totalCounter.incrementAndGet();

            Article foundDuplicateUuid = readArticles.putIfAbsent(article.getUuid(), article);

            if(foundDuplicateUuid != null) {
                duplicatedUuids.add(article.getUuid());
                continue;
            }

            String foundDuplicateTitle = seenTitles.putIfAbsent(article.getTitle(), article.getUuid());

            if(foundDuplicateTitle != null) {
                duplicatedUuids.add(article.getUuid());
                duplicatedUuids.add(foundDuplicateTitle);
            }
        }
    }

    public void cleanDuplicates() {
        for(String uuid : duplicatedUuids) {
            readArticles.remove(uuid);
        }

        activeArticlesList = new ArrayList<>(readArticles.values());
        processingIndex.set(0);
        duplicatedUuids.clear();
    }

    public void generateMaps() {
        int totalSize = activeArticlesList.size();

        while(true){
            int i = Tema1.processingIndex.getAndIncrement();

            if (i >= totalSize) {
                break;
            }

            Article currentArt = activeArticlesList.get(i);

            for(String cat : currentArt.getCategories()) {
                if(Tema1.categories.containsKey(cat))
                    Tema1.categories.get(cat).add(currentArt.getUuid());
            }

            if(Tema1.languages.containsKey(currentArt.getLanguage()))
                Tema1.languages.get(currentArt.getLanguage()).add(currentArt.getUuid());

            if("english".equals(currentArt.getLanguage())) {
                HashSet<String> uniqueWords = new HashSet<>();
                String text = currentArt.getText().toLowerCase();
                String[] words = SPACE.split(text);

                for (String word : words) {
                    if (!word.isEmpty()) {
                        uniqueWords.add(NON_ALPHA.matcher(word).replaceAll(""));
                    }
                }

                for(String word : uniqueWords) {
                    if(word.isEmpty())
                        continue;

                    if(!Tema1.forbiddenWords.contains(word)) {
                        Tema1.words.computeIfAbsent(word, k -> new AtomicInteger(0)).incrementAndGet();
                    }
                }
            }

            Tema1.authors.computeIfAbsent(currentArt.getAuthor(), k -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    @Override
    public void run() {
        while (true) {
            String shortPath = tasksQueue.poll();
            if(shortPath == null) {
                break;
            }

            runArticlesReading(shortPath);
        }

        try {
            Tema1.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(CleaningConfirmation.compareAndSet(false, true)) {
            cleanDuplicates();
        }

        try {
            Tema1.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        generateMaps();

        try {
            Tema1.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            String operation = Tema1.finalTasksQueue.poll();
            if (operation == null) {
                break;
            }

            switch (operation) {
                case "WC" -> HandleOutput.writeCategories();
                case "WL" -> HandleOutput.writeLanguages();
                case "WA" -> HandleOutput.writeArticles();
                case "WK" -> HandleOutput.writeKeywords();
                case "SA" -> HandleOutput.sortAuthors();
            }
        }

        try {
            Tema1.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(ReportsConfirmation.compareAndSet(false, true)) {
            HandleOutput.writeReports();
        }
    }
}
