package com.notes.stats;

import java.util.*;
import java.util.stream.*;

/** A tiny library: word statistics for a piece of text. */
public final class TextStats {
    private TextStats() {}

    /** Lower-cased words; anything that is not a letter or digit separates words. */
    public static List<String> words(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.toLowerCase().split("[^\\p{L}\\p{N}]+"))
                     .filter(w -> !w.isEmpty())
                     .toList();
    }

    public static int wordCount(String text) {
        return words(text).size();
    }

    /** The k most frequent words; ties are broken alphabetically. */
    public static List<Map.Entry<String, Long>> topWords(String text, int k) {
        if (k < 0) throw new IllegalArgumentException("k must be >= 0, was " + k);
        Map<String, Long> freq = words(text).stream()
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));
        return freq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(k)
                .map(e -> Map.entry(e.getKey(), e.getValue()))
                .toList();
    }
}
