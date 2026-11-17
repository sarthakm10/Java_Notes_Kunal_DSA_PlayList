package com.notes.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextStatsTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "hello world               | 2",
        "'  spaces   everywhere  ' | 2",
        "one,two;three             | 3",
        "''                        | 0"
    })
    void countsWords(String text, int expected) {
        assertEquals(expected, TextStats.wordCount(text));
    }

    @Test
    void topWordsBreaksTiesAlphabetically() {
        var top = TextStats.topWords("b a c a b d", 3);
        assertEquals(List.of(Map.entry("a", 2L), Map.entry("b", 2L), Map.entry("c", 1L)), top);
    }

    @Test
    void negativeKIsRejected() {
        var ex = assertThrows(IllegalArgumentException.class, () -> TextStats.topWords("x", -1));
        assertTrue(ex.getMessage().contains("-1"));
    }
}
