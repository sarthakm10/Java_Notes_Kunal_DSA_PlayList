package com.notes.app;

import com.notes.stats.TextStats;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String text = args.length > 0 ? String.join(" ", args)
                : "Maven builds the code, Maven runs the tests, Maven packages the code";

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("words", TextStats.wordCount(text));
        List<Map<String, Object>> top = TextStats.topWords(text, 3).stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("word", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .toList();
        report.put("top3", top);

        JsonMapper mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
        System.out.println(mapper.writeValueAsString(report));
    }
}
