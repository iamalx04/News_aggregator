package main;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class HandleOutput {
    public static List<Map.Entry<String, Set<String>>> sortedCategories;
    public static List<Map.Entry<String, Set<String>>> sortedLanguages;
    public static List<Article> sortedArticles;
    public static List<Map.Entry<String, AtomicInteger>> sortedWords;
    public static List<Map.Entry<String, AtomicInteger>> sortedAuthors;

    public static void writeCategories () {
        sortedCategories = new ArrayList<>(Tema1.categories.entrySet());
        sortedCategories.sort(Helpers::compareMapEntries);

        for(Map.Entry<String, Set<String>> cat : sortedCategories) {
            if(cat.getValue().isEmpty())
                continue;

            String filename = cat.getKey().replace(",", "").replace(" ", "_") + ".txt";

            List<String> sorted = new ArrayList<>(cat.getValue());
            sorted.sort(Comparator.naturalOrder());

            Helpers.writeToFile(filename, sorted);
        }
    }

    public static void writeLanguages () {
        sortedLanguages = new ArrayList<>(Tema1.languages.entrySet());
        sortedLanguages.sort(Helpers::compareMapEntries);

        for(Map.Entry<String, Set<String>> lang : sortedLanguages) {
            if(lang.getValue().isEmpty())
                continue;

            String filename = lang.getKey() + ".txt";

            List<String> sorted = new ArrayList<>(lang.getValue());
            sorted.sort(Comparator.naturalOrder());

            Helpers.writeToFile(filename, sorted);
        }
    }

    public static void writeArticles () {
        sortedArticles = new ArrayList<>(Tema1.readArticles.values());
        sortedArticles.sort(Helpers::compareArticles);


        List<String> allArticles = new ArrayList<>();
        for(Article article : sortedArticles) {
            allArticles.add(article.getUuid() + " " + article.getPublished());
        }
        Helpers.writeToFile("all_articles.txt", allArticles);
    }

    public static void writeKeywords () {
        sortedWords = new ArrayList<>(Tema1.words.entrySet());
        sortedWords.removeIf(entry -> entry.getValue().get() <= 0);
        sortedWords.sort(Helpers::sortWordsAndAuthors);

        List<String> allWords = new ArrayList<>();
        for(Map.Entry<String, AtomicInteger> word : sortedWords) {
            allWords.add(word.getKey() + " " + word.getValue().get());
        }
        Helpers.writeToFile("keywords_count.txt", allWords);
    }

    public static void sortAuthors () {
        sortedAuthors = new ArrayList<>(Tema1.authors.entrySet());
        sortedAuthors.removeIf(entry -> entry.getValue().get() <= 0);
        sortedAuthors.sort(Helpers::sortWordsAndAuthors);
    }

    public static void writeReports () {
        List<String> reports = new ArrayList<>();
        reports.add("duplicates_found - " + (Tema1.totalCounter.get() - Tema1.readArticles.size()));
        reports.add("unique_articles - " + Tema1.readArticles.size());
        reports.add("best_author - " + sortedAuthors.getFirst().getKey() + " " + sortedAuthors.getFirst().getValue().get());
        reports.add("top_language - " + sortedLanguages.getFirst().getKey() + " " + sortedLanguages.getFirst().getValue().size());
        reports.add("top_category - " + sortedCategories.getFirst().getKey().replace(",", "").replace(" ", "_") + " " + sortedCategories.getFirst().getValue().size());
        reports.add("most_recent_article - " + sortedArticles.getFirst().getPublished() + " " + sortedArticles.getFirst().getUrl());
        reports.add("top_keyword_en - " + sortedWords.getFirst().getKey() + " " + sortedWords.getFirst().getValue().get());
        Helpers.writeToFile("reports.txt", reports);
    }
}
